package de.fh.muenster.locationprivacytoolkit.processors.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkit
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.databinding.FragmentExclusionZoneBinding
import de.fh.muenster.locationprivacytoolkit.processors.ExclusionZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.math.roundToInt


class ExclusionZoneProcessorFragment : Fragment() {

    private lateinit var binding: FragmentExclusionZoneBinding
    private var locationPrivacyConfig: LocationPrivacyConfigManager? = null
    private var symbolManager: SymbolManager? = null
    private var lastZoneCreationCenter: LatLng? = null
    private var isInitialView = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        context?.let {
            try {
                Mapbox.getInstance(it, "sk.not_needed", WellKnownTileServer.MapLibre)
            } catch (_: Exception) {
            } catch (_: NoClassDefFoundError) {
            }
            locationPrivacyConfig = LocationPrivacyConfigManager(it)
        }

        binding = FragmentExclusionZoneBinding.inflate(inflater, container, false)

        binding.mapView.getMapAsync { map ->
            var styleBuilder = Style.Builder()
            styleBuilder.fromJson("{\n" +
                    "    \"version\": 8,\n" +
                    "    \"sources\": {\n" +
                    "      \"osm\": {\n" +
                    "        \"type\": \"raster\",\n" +
                    "        \"tiles\": [\"https://tile.openstreetmap.org/{z}/{x}/{y}.png\"],\n" +
                    "        \"tileSize\": 256,\n" +
                    "        \"attribution\": \"Map tiles by <a target=\\\"_top\\\" rel=\\\"noopener\\\" href=\\\"https://tile.openstreetmap.org/\\\">OpenStreetMap tile servers</a>, under the <a target=\\\"_top\\\" rel=\\\"noopener\\\" href=\\\"https://operations.osmfoundation.org/policies/tiles/\\\">tile usage policy</a>. Data by <a target=\\\"_top\\\" rel=\\\"noopener\\\" href=\\\"http://openstreetmap.org\\\">OpenStreetMap</a>\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"layers\": [{\n" +
                    "      \"id\": \"osm\",\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"source\": \"osm\"\n" +
                    "    }]\n" +
                    "}");
            map.setStyle(styleBuilder)
            centerMapTo(map, INITIAL_LATITUDE, INITIAL_LONGITUDE, INITIAL_ZOOM)
        }

        binding.addZoneButton.setOnClickListener {
            showZoneCreationOverlay()
        }
        binding.removeZonesButton.setOnClickListener {
            removeAllZones()
        }
        binding.exclusionZoneCardCloseButton.setOnClickListener {
            hideZoneCreationOverlay()
        }
        binding.exclusionZoneCardCreateButton.setOnClickListener {
            createZone()
        }
        binding.exclusionZoneSlider.valueFrom = MIN_ZONE_RADIUS
        binding.exclusionZoneSlider.valueTo = MAX_ZONE_RADIUS
        binding.exclusionZoneSlider.value = INITIAL_ZONE_RADIUS
        binding.exclusionZoneSlider.stepSize = ZONE_STEP_SIZE
        binding.exclusionZoneSlider.setLabelFormatter { value -> "${value.roundToInt()} m" }
        binding.exclusionZoneSlider.addOnChangeListener { _, _, _ ->
            updateCreationZone(lastZoneCreationCenter)
        }
        reloadExclusionZones()

        activity?.setTitle(R.string.exclusionZoneTitle)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        broadcastUpdate()
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    private fun centerMapTo(map: MapboxMap, lat: Double, lon: Double, zoom: Double? = null) {
        val camera =
            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom ?: map.cameraPosition.zoom)
        map.easeCamera(camera)
    }

    private fun showZoneCreationOverlay() {
        binding.exclusionZoneCard.visibility = View.VISIBLE
        binding.addZoneButton.visibility = View.GONE
        binding.removeZonesButton.visibility = View.GONE

        // add zone creation content to map
        binding.mapView.getMapAsync { map ->
            map.cameraPosition.target?.let { center ->
                map.style?.let { style ->
                    // add zone creation layer
                    val zoneCreationCircle =
                        createMapCirclePolygon(center, binding.exclusionZoneSlider.value.toDouble())
                    style.addSource(GeoJsonSource(ZONE_CREATION_SOURCE).apply {
                        setGeoJson(zoneCreationCircle)
                    })
                    val zoneCreationLayer =
                        FillLayer(ZONE_CREATION_LAYER, ZONE_CREATION_SOURCE).withProperties(
                            PropertyFactory.fillColor(ZONE_CREATION_COLOR),
                            PropertyFactory.fillOpacity(ZONE_LAYER_OPACITY),
                            PropertyFactory.fillOutlineColor(ZONE_CREATION_COLOR),
                        )
                    val zoneCreationLineLayer =
                        LineLayer(ZONE_CREATION_OUTLINE_LAYER, ZONE_CREATION_SOURCE).withProperties(
                            PropertyFactory.lineColor(ZONE_CREATION_COLOR),
                            PropertyFactory.lineWidth(ZONE_OUTLINE_WIDTH),
                        )
                    style.addLayer(zoneCreationLayer)
                    style.addLayerAbove(zoneCreationLineLayer, ZONE_CREATION_LAYER)

                    // add zone creation marker
                    context?.let { c ->
                        AppCompatResources.getDrawable(c, R.drawable.ic_move)?.let { image ->
                            style.addImage(ZONE_CREATION_IMAGE, image)
                        }
                    }
                    symbolManager = SymbolManager(binding.mapView, map, style)
                    symbolManager?.iconAllowOverlap = true
                    symbolManager?.iconIgnorePlacement = true
                    val symbolOptions =
                        SymbolOptions().withLatLng(center).withIconImage(ZONE_CREATION_IMAGE)
                            .withIconSize(2f).withDraggable(true)
                    symbolManager?.create(symbolOptions)
                    lastZoneCreationCenter = center
                    symbolManager?.addDragListener(object : OnSymbolDragListener {
                        override fun onAnnotationDragStarted(annotation: Symbol) = Unit
                        override fun onAnnotationDrag(annotation: Symbol) {
                            updateCreationZone(annotation.latLng)
                        }

                        override fun onAnnotationDragFinished(annotation: Symbol) {
                            lastZoneCreationCenter = annotation.latLng
                        }
                    })
                }
            }
        }
    }

    private fun updateCreationZone(latLng: LatLng?) {
        val position = latLng ?: return
        binding.mapView.getMapAsync { map ->
            map.style?.let { style ->
                (style.getSource(ZONE_CREATION_SOURCE) as? GeoJsonSource)?.let { source ->
                    val newCircle = createMapCirclePolygon(
                        position, binding.exclusionZoneSlider.value.toDouble()
                    )
                    source.setGeoJson(newCircle)
                }
            }
        }
    }

    private fun hideZoneCreationOverlay() {
        binding.exclusionZoneCard.visibility = View.GONE
        binding.addZoneButton.visibility = View.VISIBLE

        // remove zone creation content from map
        symbolManager?.deleteAll()
        binding.mapView.getMapAsync { map ->
            map.style?.let { style ->
                // remove creation layer and source
                style.removeLayer(ZONE_CREATION_LAYER)
                style.removeLayer(ZONE_CREATION_OUTLINE_LAYER)
                style.removeSource(ZONE_CREATION_SOURCE)
            }
        }
        reloadExclusionZones()
    }

    private fun createZone() {
        lastZoneCreationCenter?.let { center ->
            val radius = binding.exclusionZoneSlider.value
            val zone = ExclusionZone(center, radius.toInt())
            addExclusionZone(zone)
        }
        hideZoneCreationOverlay()
    }

    private fun removeAllZones() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentZones = loadExclusionZones()
            locationPrivacyConfig?.setPrivacyConfig(LocationPrivacyConfig.ExclusionZone, "")
            withContext(Dispatchers.Main) {
                binding.mapView.getMapAsync { map ->
                    map.getStyle { style ->
                        // remove zones
                        style.removeLayer(ZONE_LAYER)
                        style.removeLayer(ZONE_OUTLINE_LAYER)
                        style.removeSource(ZONE_SOURCE)
                    }
                }
                showDeletionToast(currentZones)
            }
        }
        binding.removeZonesButton.visibility = View.GONE
    }

    private fun addZonesToMap(zones: List<ExclusionZone>) {
        binding.mapView.getMapAsync { map ->
            map.getStyle { style ->
                // remove old zones
                style.removeLayer(ZONE_LAYER)
                style.removeLayer(ZONE_OUTLINE_LAYER)
                style.removeSource(ZONE_SOURCE)

                // add new zones
                val zonePolygons = zones.map { z ->
                    createMapCirclePolygon(z.center, z.radiusMeters.toDouble())
                }
                val zoneFeatureCollection =
                    FeatureCollection.fromFeatures(zonePolygons.map { p -> Feature.fromGeometry(p) })
                style.addSource(GeoJsonSource(ZONE_SOURCE).apply {
                    setGeoJson(zoneFeatureCollection)
                })
                val zoneLayer = FillLayer(ZONE_LAYER, ZONE_SOURCE).withProperties(
                    PropertyFactory.fillColor(ZONE_COLOR),
                    PropertyFactory.fillOpacity(ZONE_LAYER_OPACITY),
                    PropertyFactory.fillOutlineColor(ZONE_COLOR),
                )
                val zoneLineLayer = LineLayer(ZONE_OUTLINE_LAYER, ZONE_SOURCE).withProperties(
                    PropertyFactory.lineColor(ZONE_COLOR),
                    PropertyFactory.lineWidth(ZONE_OUTLINE_WIDTH),
                )
                style.addLayer(zoneLayer)
                style.addLayerAbove(zoneLineLayer, ZONE_LAYER)

                if (isInitialView) {
                    isInitialView = false
                    val padding = intArrayOf(
                        INITIAL_PADDING, INITIAL_PADDING, INITIAL_PADDING, INITIAL_PADDING
                    )
                    val geometry = MultiPolygon.fromPolygons(zonePolygons)
                    map.getCameraForGeometry(geometry, padding)?.let { cameraPosition ->
                        map.cameraPosition = cameraPosition
                    }
                }
            }
        }
    }

    private fun createMapCirclePolygon(center: LatLng, radiusMeters: Double): Polygon {
        return TurfTransformation.circle(
            Point.fromLngLat(center.longitude, center.latitude),
            radiusMeters,
            TurfConstants.UNIT_METERS
        )
    }

    private fun reloadExclusionZones() {
        val zones = loadExclusionZones()
        zones?.let { addZonesToMap(it) } ?: run {
            isInitialView = false
        }
        binding.removeZonesButton.visibility =
            if (zones?.isEmpty() != false) View.GONE else View.VISIBLE
    }

    private fun addExclusionZone(zone: ExclusionZone) {
        CoroutineScope(Dispatchers.IO).launch {
            val zones = loadExclusionZones()?.toMutableList() ?: mutableListOf()
            zones.add(zone)
            val zonesJson = Gson().toJson(zones)
            locationPrivacyConfig?.setPrivacyConfig(LocationPrivacyConfig.ExclusionZone, zonesJson)
            withContext(Dispatchers.Main) {
                reloadExclusionZones()
            }
        }
    }

    private fun loadExclusionZones(): List<ExclusionZone>? {
        locationPrivacyConfig?.getPrivacyConfigString(LocationPrivacyConfig.ExclusionZone)
            ?.let { zonesJson ->
                val zoneListType = object : TypeToken<List<ExclusionZone>>() {}.type
                return try {
                    Gson().fromJson(zonesJson, zoneListType) as? List<ExclusionZone>
                } catch (_: JsonSyntaxException) {
                    null
                }
            }
        return null
    }

    private fun showDeletionToast(deletedZones: List<ExclusionZone>?) {
        val snackbar =
            Snackbar.make(binding.root, R.string.exclusionZonesDeletedMessage, Snackbar.LENGTH_LONG)
        snackbar.setAction(
            R.string.exclusionZonesDeleteUndo
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val zonesJson = Gson().toJson(deletedZones)
                locationPrivacyConfig?.setPrivacyConfig(
                    LocationPrivacyConfig.ExclusionZone, zonesJson
                )
                withContext(Dispatchers.Main) {
                    reloadExclusionZones()
                }
            }
        }
        snackbar.show()
    }

    private fun broadcastUpdate() {
        Intent().also { intent ->
            intent.action = ACTION_EXCLUSION_ZONE_UPDATE
            activity?.sendBroadcast(intent)
        }
    }


    companion object {
        const val ACTION_EXCLUSION_ZONE_UPDATE =
            "de.fh.muenster.locationprivacytoolkit.EXCLUSION_ZONE_UPDATE"

        // roughly Münster Westf.
        private const val INITIAL_LATITUDE = 51.961563
        private const val INITIAL_LONGITUDE = 7.628202
        private const val INITIAL_ZOOM = 3.0
        private const val INITIAL_PADDING = 300

        private const val MIN_ZONE_RADIUS = 100f
        private const val INITIAL_ZONE_RADIUS = 500f
        private const val MAX_ZONE_RADIUS = 10000f
        private const val ZONE_STEP_SIZE = 100f

        private const val ZONE_LAYER_OPACITY = 0.5f
        private const val ZONE_OUTLINE_WIDTH = 3f
        private const val ZONE_COLOR = Color.GRAY
        private const val ZONE_LAYER = "exclusion_zone_layer"
        private const val ZONE_OUTLINE_LAYER = "exclusion_zone_outline_layer"
        private const val ZONE_SOURCE = "exclusion_zone_source"

        private const val ZONE_CREATION_COLOR = Color.RED
        private const val ZONE_CREATION_IMAGE = "add_zone_image"
        private const val ZONE_CREATION_LAYER = "exclusion_zone_creation_layer"
        private const val ZONE_CREATION_OUTLINE_LAYER = "exclusion_zone_creation_outline_layer"
        private const val ZONE_CREATION_SOURCE = "exclusion_zone_creation_source"
    }
}