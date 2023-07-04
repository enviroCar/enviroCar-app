package de.fh.muenster.locationprivacytoolkit.processors.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkit
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.databinding.FragmentLocationHistoryBinding
import de.fh.muenster.locationprivacytoolkit.processors.db.LocationPrivacyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date


private enum class HistoryMapContentMode {
    Timeline, Heatmap
}

private enum class HistoryMapFilterMode {
    Area, Time
}

private enum class HistoryMapTouchMode {
    Move, Draw
}

class HistoryProcessorFragment : Fragment() {

    private lateinit var binding: FragmentLocationHistoryBinding
    private var locationDatabase: LocationPrivacyDatabase? = null
    private var locationPrivacyConfig: LocationPrivacyConfigManager? = null
    private var lastLocations: List<Location>? = null
    private var isLayersFabExtended = false
    private val useExampleData: Boolean
        get() = locationPrivacyConfig?.getUseExampleData() ?: false
    private val firstLocationTime: Long?
        get() = lastLocations?.minBy { l -> l.time }?.time
    private val lastLocationTime: Long?
        get() = lastLocations?.maxBy { l -> l.time }?.time

    // map modes
    private var mapContentMode: HistoryMapContentMode = HistoryMapContentMode.Timeline
    private var mapTouchMode: HistoryMapTouchMode = HistoryMapTouchMode.Move
        @SuppressLint("ClickableViewAccessibility") set(value) {
            field = value
            when (value) {
                HistoryMapTouchMode.Move -> {
                    binding.mapView.setOnTouchListener(null)
                    if (areaFilterPolygon.isNotEmpty()) {
                        binding.filterCardCreateAreaButton.visibility = View.GONE
                        binding.filterCardClearAreaButton.visibility = View.VISIBLE
                        binding.filterCardAreaInfoText.visibility = View.VISIBLE
                    } else {
                        binding.filterCardCreateAreaButton.visibility = View.VISIBLE
                        binding.filterCardClearAreaButton.visibility = View.GONE
                        binding.filterCardAreaInfoText.visibility = View.GONE
                    }
                    binding.filterCardAreaHintText.visibility = View.GONE
                }

                HistoryMapTouchMode.Draw -> {
                    // clear previous selection
                    updateAreaFilterOnMap(hide = true)
                    areaFilterOutline.clear()
                    areaFilterPolygon.clear()
                    binding.mapView.setOnTouchListener(drawTouchListener)
                    binding.filterCardCreateAreaButton.visibility = View.GONE
                    binding.filterCardAreaHintText.visibility = View.VISIBLE
                    binding.filterCardAreaInfoText.visibility = View.GONE
                }
            }
        }
    private var mapFilterMode: HistoryMapFilterMode = HistoryMapFilterMode.Time
        set(value) {
            field = value
            when (value) {
                HistoryMapFilterMode.Time -> {
                    // reset map touch mode
                    mapTouchMode = HistoryMapTouchMode.Move
                    updateAreaFilterOnMap(hide = true)
                    updateMarkedLocationsOnMap()
                    updateTimeFilterLabel()
                }

                HistoryMapFilterMode.Area -> {
                    updateAreaFilterOnMap(hide = false)
                    updateMarkedLocationsOnMap()
                }
            }
        }

    // filter
    private val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    private var timeFilterRange: LongRange? = null
    private val areaFilterPolygon = mutableListOf<Point>()
    private val areaFilterOutline = mutableListOf<Point>()

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
            locationDatabase = LocationPrivacyDatabase.sharedInstance(it)
        }

        binding = FragmentLocationHistoryBinding.inflate(inflater, container, false)
        binding.mapView.getMapAsync { map ->
            map.setStyle(LocationPrivacyToolkit.mapTilesUrl)
            loadLocations()
        }

        initMapSourcesAndLayers()
        initFloatingActionButtons()
        initFilterOptions()

        activity?.setTitle(R.string.historyTitle)
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
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    private fun initFloatingActionButtons() {
        updateLayerFabState()
        binding.timeLineLayerFab.visibility = View.GONE
        binding.timeLineLayerFabText.visibility = View.GONE
        binding.heatmapLayerFab.visibility = View.GONE
        binding.heatmapLayerFabText.visibility = View.GONE
        binding.layersFab.shrink()

        binding.layersFab.setOnClickListener {
            if (isLayersFabExtended) {
                isLayersFabExtended = false
                binding.timeLineLayerFab.hide()
                binding.heatmapLayerFab.hide()
                binding.layersFab.shrink()
                binding.timeLineLayerFabText.visibility = View.GONE
                binding.heatmapLayerFabText.visibility = View.GONE
            } else {
                isLayersFabExtended = true
                binding.timeLineLayerFab.show()
                binding.heatmapLayerFab.show()
                binding.timeLineLayerFabText.visibility = View.VISIBLE
                binding.heatmapLayerFabText.visibility = View.VISIBLE
                binding.layersFab.extend()
            }
        }

        binding.timeLineLayerFab.setOnClickListener {
            changeMapContentMode(HistoryMapContentMode.Timeline)
        }
        binding.timeLineLayerFabText.setOnClickListener {
            changeMapContentMode(HistoryMapContentMode.Timeline)
        }

        binding.heatmapLayerFab.setOnClickListener {
            changeMapContentMode(HistoryMapContentMode.Heatmap)
        }
        binding.heatmapLayerFabText.setOnClickListener {
            changeMapContentMode(HistoryMapContentMode.Heatmap)
        }
    }

    private fun initFilterOptions() {
        binding.filterCardCloseButton.setOnClickListener {
            toggleFilterCard()
        }
        binding.filterFab.setOnClickListener {
            toggleFilterCard()
        }

        binding.filterCardToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            when (checkedId) {
                binding.filterCardTimeToggleButton.id -> {
                    if (isChecked) {
                        binding.filterCardTimeLayout.visibility = View.VISIBLE
                        mapFilterMode = HistoryMapFilterMode.Time
                    } else {
                        binding.filterCardTimeLayout.visibility = View.GONE
                    }
                }

                binding.filterCardAreaToggleButton.id -> {
                    if (isChecked) {
                        binding.filterCardAreaLayout.visibility = View.VISIBLE
                        mapFilterMode = HistoryMapFilterMode.Area
                    } else {
                        binding.filterCardAreaLayout.visibility = View.GONE
                    }
                }

                else -> return@addOnButtonCheckedListener
            }
        }
        binding.filterCardToggleGroup.check(binding.filterCardTimeToggleButton.id)

        binding.filterCardClearAreaButton.setOnClickListener {
            areaFilterOutline.clear()
            areaFilterPolygon.clear()
            updateAreaFilterOnMap()
            updateMarkedLocationsOnMap()
            binding.filterCardClearAreaButton.visibility = View.GONE
            binding.filterCardAreaInfoText.visibility = View.GONE
            binding.filterCardCreateAreaButton.visibility = View.VISIBLE
        }
        binding.filterCardDeleteButton.setOnClickListener {
            deleteMarkedLocations()
        }

        binding.filterCardCreateAreaButton.setOnClickListener {
            mapTouchMode = HistoryMapTouchMode.Draw
        }
        binding.filterCardTimeDateRangeStart.setOnClickListener {
            showDatePicker(isStart = true)
        }
        binding.filterCardTimeDateRangeEnd.setOnClickListener {
            showDatePicker(isStart = false)
        }
        binding.filterCard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateMapPadding() }
    }

    private fun initMapSourcesAndLayers() {
        binding.mapView.getMapAsync { map ->
            map.getStyle { style ->
                // locations
                style.addSource(GeoJsonSource(LOCATIONS_SOURCE))
                style.addLayer(
                    CircleLayer(
                        LOCATIONS_LAYER, LOCATIONS_SOURCE
                    ).withProperties(
                        PropertyFactory.circleColor(LOCATIONS_COLOR),
                        PropertyFactory.circleRadius(LOCATIONS_SIZE),
                        PropertyFactory.circleOpacity(LOCATIONS_OPACITY),
                        PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                        PropertyFactory.circleStrokeColor(LOCATIONS_STROKE_COLOR),
                        PropertyFactory.circleStrokeWidth(
                            Expression.step(
                                Expression.zoom(), Expression.literal(
                                    LOCATIONS_MIN_STROKE_SIZE
                                ),
                                Expression.stop(LOCATIONS_ZOOM_THRES, LOCATIONS_MAX_STROKE_SIZE)
                            )
                        )
                    )
                )
                style.addLayerAbove(
                    HeatmapLayer(
                        LOCATIONS_HEATMAP_LAYER, LOCATIONS_SOURCE
                    ), LOCATIONS_LAYER
                )

                // marked locations
                style.addSource(GeoJsonSource(LOCATIONS_MARKED_SOURCE))
                style.addLayerAbove(
                    CircleLayer(
                        LOCATIONS_MARKED_LAYER, LOCATIONS_MARKED_SOURCE
                    ).withProperties(
                        PropertyFactory.circleColor(LOCATIONS_MARKED_COLOR),
                        PropertyFactory.circleRadius(LOCATIONS_SIZE),
                        PropertyFactory.circleOpacity(LOCATIONS_OPACITY),
                        PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                        PropertyFactory.circleStrokeColor(LOCATIONS_STROKE_COLOR),
                        PropertyFactory.circleStrokeWidth(
                            Expression.step(
                                Expression.zoom(), Expression.literal(
                                    LOCATIONS_MIN_STROKE_SIZE
                                ),
                                Expression.stop(LOCATIONS_ZOOM_THRES, LOCATIONS_MAX_STROKE_SIZE)
                            )
                        )
                    ), LOCATIONS_HEATMAP_LAYER
                )

                // area filter
                style.addSource(GeoJsonSource(AREA_FILTER_LINE_SOURCE))
                style.addSource(GeoJsonSource(AREA_FILTER_FILL_SOURCE))
                style.addLayerBelow(
                    LineLayer(
                        AREA_FILTER_LINE_LAYER, AREA_FILTER_LINE_SOURCE
                    ).withProperties(
                        PropertyFactory.lineWidth(AREA_FILTER_LINE_WIDTH),
                        PropertyFactory.lineJoin(LINE_JOIN_ROUND),
                        PropertyFactory.lineOpacity(AREA_FILTER_LINE_OPACITY),
                        PropertyFactory.lineColor(Color.parseColor(AREA_FILTER_LINE_COLOR))
                    ),
                    LOCATIONS_MARKED_LAYER
                )
                style.addLayerBelow(
                    FillLayer(
                        AREA_FILTER_FILL_LAYER, AREA_FILTER_FILL_SOURCE
                    ).withProperties(
                        PropertyFactory.fillColor(Color.RED),
                        PropertyFactory.fillOpacity(AREA_FILTER_FILL_OPACITY)
                    ), AREA_FILTER_LINE_LAYER
                )
            }
        }
    }

    private fun toggleFilterCard() {
        val showFilter = binding.filterCard.visibility == View.GONE
        binding.filterCard.visibility = if (showFilter) View.VISIBLE else View.GONE
        binding.filterFab.visibility = if (showFilter) View.GONE else View.VISIBLE
        if (showFilter && timeFilterRange == null) {
            val startTime = firstLocationTime ?: Date().time
            val endTime =
                lastLocationTime ?: (startTime + 86400L) // fallback: startTime plus one day
            timeFilterRange = LongRange(startTime, endTime)
            updateTimeFilterLabel()
        }
        updateMapPadding()
        if (mapFilterMode == HistoryMapFilterMode.Area) {
            updateAreaFilterOnMap(hide = !showFilter)
        }
        updateMarkedLocationsOnMap(hide = !showFilter)
    }

    private fun changeMapContentMode(newMode: HistoryMapContentMode) {
        val locations = lastLocations ?: return
        if (newMode != mapContentMode) {
            mapContentMode = newMode
            updateMapLocations(locations)
            updateLayerFabState()
        }
    }

    private fun updateLayerFabState() {
        when (mapContentMode) {
            HistoryMapContentMode.Timeline -> {
                binding.timeLineLayerFabText.typeface = Typeface.DEFAULT_BOLD
                binding.heatmapLayerFabText.typeface = Typeface.DEFAULT
            }

            HistoryMapContentMode.Heatmap -> {
                binding.timeLineLayerFabText.typeface = Typeface.DEFAULT
                binding.heatmapLayerFabText.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val dateRangePicker = MaterialDatePicker.Builder.datePicker().apply {
            setTitleText(R.string.historyFilterByTimeDateHint)
            timeFilterRange?.let { range ->
                val selection = if (isStart) range.first else range.last
                setSelection(selection)
            }
        }.build()
        dateRangePicker.addOnPositiveButtonClickListener { date ->
            val startDate = if (isStart) date else timeFilterRange?.first ?: 0L
            val endDate = if (isStart) timeFilterRange?.last ?: 0L else date
            timeFilterRange = LongRange(startDate, endDate)
            updateTimeFilterLabel()
            updateMarkedLocationsOnMap()
        }
        dateRangePicker.show(parentFragmentManager, null)
    }

    private fun updateTimeFilterLabel() {
        timeFilterRange?.let { range ->
            val startDate = Date(range.first)
            val endDate = Date(range.last)
            binding.filterCardTimeDateRangeStart.text = dateFormat.format(startDate)
            binding.filterCardTimeDateRangeEnd.text = dateFormat.format(endDate)
        }
    }

    private fun loadLocations(updateCamera: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            lastLocations = locationDatabase?.loadLocations(useExampleData) ?: emptyList()
            withContext(Dispatchers.Main) {
                lastLocations?.let { locations ->
                    if (locations.isNotEmpty()) {
                        updateMapLocations(locations, updateCamera)
                        return@withContext
                    }
                }
                if (updateCamera) {
                    // fallback: default initial zoom
                    binding.mapView.getMapAsync { map ->
                        val initialLatLng = LatLng(
                            INITIAL_LATITUDE, INITIAL_LONGITUDE
                        )
                        val camera = CameraUpdateFactory.newLatLngZoom(
                            initialLatLng, INITIAL_ZOOM
                        )
                        map.easeCamera(camera)
                    }
                }
            }
        }
    }

    private fun deleteMarkedLocations() {
        context?.let { c ->
            val locations = lastLocations ?: return
            val filteredLocations = filterLocations(locations)
            val message =
                String.format(getString(R.string.historyDeleteAlertMessage), filteredLocations.size)
            val dialog = MaterialAlertDialogBuilder(c).setTitle(R.string.historyDeleteAlertTitle)
                .setMessage(message)
                .setNegativeButton(R.string.historyCancelAlertButton, null)
                .setPositiveButton(R.string.historyDeleteAlertButton) { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        locationDatabase?.remove(filteredLocations)

                        // update ui and map
                        withContext(Dispatchers.Main) {
                            toggleFilterCard()
                        }
                        loadLocations(updateCamera = false)
                        areaFilterOutline.clear()
                        areaFilterPolygon.clear()
                        updateAreaFilterOnMap()
                        updateMarkedLocationsOnMap()

                        // show deletion snack bar
                        val snackMessage =
                            String.format(
                                getString(R.string.historyDeletedMessage),
                                filteredLocations.size
                            )
                        val bar =
                            Snackbar.make(
                                binding.root,
                                snackMessage,
                                Snackbar.LENGTH_LONG
                            )
                        withContext(Dispatchers.Main) {
                            bar.show()
                        }
                    }

                }
            dialog.show()
        }
    }

    private fun filterLocations(locations: List<Location>): List<Location> {
        return try {
            when (mapFilterMode) {
                HistoryMapFilterMode.Time -> {
                    timeFilterRange?.let { range ->
                        locations.filter { l -> l.time >= range.first && l.time <= range.last }
                    } ?: locations
                }

                HistoryMapFilterMode.Area -> {
                    val polygonPoints = areaFilterPolygon.toList()
                    val polygon: Polygon = Polygon.fromLngLats(listOf(polygonPoints))
                    locations.filter { l ->
                        TurfJoins.inside(Point.fromLngLat(l.longitude, l.latitude), polygon)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun updateMapLocations(locations: List<Location>, updateCamera: Boolean = true) {
        binding.mapView.getMapAsync { map ->
            map.getStyle { style ->
                // add locations  layer
                val locationPoints = locations.map { l ->
                    Point.fromLngLat(
                        l.longitude, l.latitude
                    )
                }
                val locationMultiPoint = MultiPoint.fromLngLats(locationPoints)
                style.getSourceAs<GeoJsonSource>(LOCATIONS_SOURCE)?.setGeoJson(locationMultiPoint)
                when (mapContentMode) {
                    HistoryMapContentMode.Timeline -> {
                        style.getLayer(LOCATIONS_LAYER)
                            ?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                        style.getLayer(LOCATIONS_HEATMAP_LAYER)
                            ?.setProperties(PropertyFactory.visibility(Property.NONE))
                    }

                    HistoryMapContentMode.Heatmap -> {
                        style.getLayer(LOCATIONS_LAYER)
                            ?.setProperties(PropertyFactory.visibility(Property.NONE))
                        style.getLayer(LOCATIONS_HEATMAP_LAYER)
                            ?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                    }
                }
                if (updateCamera) {
                    val bounds = LatLngBounds.fromLatLngs(locations.map { l -> LatLng(l) })
                    val update = CameraUpdateFactory.newLatLngBounds(bounds, LOCATIONS_PADDING)
                    map.easeCamera(update)
                }
            }
        }
    }

    private fun updateMapPadding() {
        binding.mapView.getMapAsync { map ->
            val topPadding =
                if (binding.filterCard.visibility == View.VISIBLE) binding.filterCard.height.toDouble() else 0.0
            map.easeCamera(
                CameraUpdateFactory.paddingTo(
                    0.0, topPadding, 0.0, 0.0
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val drawTouchListener = OnTouchListener { _, motionEvent ->
        binding.mapView.getMapAsync { map ->
            val latLngTouchCoordinate: LatLng = map.projection.fromScreenLocation(
                PointF(motionEvent.x, motionEvent.y)
            )
            val screenTouchPoint = Point.fromLngLat(
                latLngTouchCoordinate.longitude, latLngTouchCoordinate.latitude
            )

            // update polygon and outline
            areaFilterOutline.add(screenTouchPoint)
            if (areaFilterPolygon.size < 2) {
                areaFilterPolygon.add(screenTouchPoint)
            } else {
                if (areaFilterPolygon.size > 2) {
                    areaFilterPolygon.removeAt(
                        areaFilterPolygon.size - 1
                    )
                }
                areaFilterPolygon.add(screenTouchPoint)
                areaFilterPolygon.add(
                    areaFilterPolygon[0]
                )
            }
            // draw polygon and outline
            updateAreaFilterOnMap()
            updateMarkedLocationsOnMap()

            if (motionEvent.action == MotionEvent.ACTION_UP) {
                // add the first screen touch point to the end of the outline
                areaFilterOutline.add(
                    areaFilterOutline[0]
                )

                // drawing is done, reset to move mode
                mapTouchMode = HistoryMapTouchMode.Move
            }
        }
        true
    }

    private fun updateAreaFilterOnMap(hide: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val polyline = LineString.fromLngLats(
                if (hide) emptyList<Point>() else areaFilterOutline
            )
            val polygon: Polygon =
                Polygon.fromLngLats(listOf(if (hide) emptyList<Point>() else areaFilterPolygon))
            val area = (TurfMeasurement.area(polygon) / 1000000.0)

            withContext(Dispatchers.Main)
            {
                binding.mapView.getMapAsync { map ->
                    map.style?.let { style ->
                        style.getSourceAs<GeoJsonSource>(AREA_FILTER_LINE_SOURCE)?.setGeoJson(
                            polyline
                        )
                        style.getSourceAs<GeoJsonSource>(AREA_FILTER_FILL_SOURCE)?.setGeoJson(
                            polygon
                        )
                    }
                }
                if (area > 0.0) {
                    val df = DecimalFormat("#.##")
                    df.roundingMode = RoundingMode.CEILING
                    withContext(Dispatchers.Main) {
                        binding.filterCardAreaInfoText.text = "~${df.format(area)} km²"
                        binding.filterCardAreaHintText.visibility = View.GONE
                        binding.filterCardAreaInfoText.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun updateMarkedLocationsOnMap(hide: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val locations = if (hide) emptyList() else lastLocations ?: emptyList()
            val locationPoints = filterLocations(locations).map { l ->
                Point.fromLngLat(
                    l.longitude, l.latitude
                )
            }
            val markedLocations = MultiPoint.fromLngLats(locationPoints)

            withContext(Dispatchers.Main)
            {
                binding.mapView.getMapAsync { map ->
                    map.style?.let { style ->
                        style.getSourceAs<GeoJsonSource>(LOCATIONS_MARKED_SOURCE)
                            ?.setGeoJson(markedLocations)
                    }
                }

                binding.filterCardDeleteButton.isEnabled = locationPoints.isNotEmpty()
            }
        }
    }

    companion object {

        // roughly Münster Westf.
        private const val INITIAL_LATITUDE = 51.961563
        private const val INITIAL_LONGITUDE = 7.628202
        private const val INITIAL_ZOOM = 3.0

        // general locations
        private const val LOCATIONS_LAYER = "locations_layer"
        private const val LOCATIONS_HEATMAP_LAYER = "locations_heatmap_layer"
        private const val LOCATIONS_SOURCE = "locations_source"
        private const val LOCATIONS_MARKED_LAYER = "locations_marked_layer"
        private const val LOCATIONS_MARKED_SOURCE = "locations_marked_source"
        private const val LOCATIONS_COLOR = Color.BLUE
        private const val LOCATIONS_STROKE_COLOR = Color.WHITE
        private const val LOCATIONS_MARKED_COLOR = Color.RED
        private const val LOCATIONS_SIZE = 8f
        private const val LOCATIONS_OPACITY = 0.7f
        private const val LOCATIONS_MIN_STROKE_SIZE = .5f
        private const val LOCATIONS_MAX_STROKE_SIZE = 2f
        private const val LOCATIONS_ZOOM_THRES = 13f
        private const val LOCATIONS_PADDING = 100

        // area filter selection
        private const val AREA_FILTER_LINE_SOURCE = "area_filter_line_source"
        private const val AREA_FILTER_FILL_SOURCE = "area_filter_fill_source"
        private const val AREA_FILTER_LINE_LAYER = "area_filter_line_layer"
        private const val AREA_FILTER_FILL_LAYER = "area_filter_fill_layer"
        private const val AREA_FILTER_LINE_COLOR = "#a0861c"
        private const val AREA_FILTER_LINE_WIDTH = 5f
        private const val AREA_FILTER_LINE_OPACITY = 1f
        private const val AREA_FILTER_FILL_OPACITY = .4f
    }
}