package org.envirocar.map.provider.maplibre

import android.graphics.Color
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.update
import org.envirocar.map.MapController
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.camera.MutableCameraState
import org.envirocar.map.model.Animation
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polygon
import org.envirocar.map.model.Polyline
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Fill
import org.maplibre.android.plugins.annotation.FillManager
import org.maplibre.android.plugins.annotation.FillOptions
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.lineProgress
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineGradient
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString

/**
 * [MapLibreMapController]
 * -----------------------
 * [MapLibre](https://www.maplibre.org) based implementation for [MapController].
 */
internal class MapLibreMapController(private val viewInstance: MapView) : MapController() {
    override val camera = MutableCameraState()
    private val markers = mutableMapOf<Long, Symbol>()
    private val polygons = mutableMapOf<Long, Fill>()
    private val polylines = mutableSetOf<Long>()

    // Symbol = Marker
    // Fill = Polygon
    private lateinit var symbolManager: SymbolManager
    private lateinit var fillManager: FillManager

    private lateinit var mapLibreMap: MapLibreMap
    private lateinit var style: Style

    init {
        viewInstance.getMapAsync { mapLibreMap ->
            this.mapLibreMap = mapLibreMap

            // Disable attribution, compass & logo.
            mapLibreMap.uiSettings.isAttributionEnabled = false
            mapLibreMap.uiSettings.isCompassEnabled = false
            mapLibreMap.uiSettings.isLogoEnabled = false

            // Once the map style is loaded, make the view visible & mark this instance as ready.
            if (mapLibreMap.style != null) {
                style = mapLibreMap.style!!
                viewInstance.visibility = View.VISIBLE
                symbolManager = SymbolManager(viewInstance, mapLibreMap, style)
                fillManager = FillManager(viewInstance, mapLibreMap, style)
                readyCompletableDeferred.complete(Unit)
            } else {
                mapLibreMap.getStyle {
                    style = it
                    viewInstance.visibility = View.VISIBLE
                    symbolManager = SymbolManager(viewInstance, mapLibreMap, style)
                    fillManager = FillManager(viewInstance, mapLibreMap, style)
                    readyCompletableDeferred.complete(Unit)
                }
            }

            // Forward the camera events from Mapbox to CameraState in MapController.
            mapLibreMap.addOnCameraMoveListener {
                camera.position.update { mapLibreMap.cameraPosition.target!!.toPoint() }
                camera.bearing.update { mapLibreMap.cameraPosition.bearing.toFloat() }
                camera.tilt.update { mapLibreMap.cameraPosition.tilt.toFloat() }
                camera.zoom.update { mapLibreMap.cameraPosition.zoom.toFloat() }
            }
        }
    }

    override fun setMinZoom(minZoom: Float) = runWhenReady {
        super.setMinZoom(minZoom)
        mapLibreMap.setMinZoomPreference(minZoom.toMapLibreZoom())
    }

    override fun setMaxZoom(maxZoom: Float) = runWhenReady {
        super.setMaxZoom(maxZoom)
        mapLibreMap.setMaxZoomPreference(maxZoom.toMapLibreZoom())
    }

    override fun notifyCameraUpdate(cameraUpdate: CameraUpdate, animation: Animation?) =
        runWhenReady {
            super.notifyCameraUpdate(cameraUpdate, animation)

            fun setOrEaseCamera(cameraUpdate: org.maplibre.android.camera.CameraUpdate) {
                if (animation == null) {
                    mapLibreMap.moveCamera(cameraUpdate)
                } else {
                    mapLibreMap.easeCamera(cameraUpdate, animation.duration.toInt())
                }
            }

            with(cameraUpdate) {
                when (this) {
                    is CameraUpdate.Companion.CameraUpdateBasedOnBounds -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.fromLatLngs(points.map { it.toMapLibreLatLng() }),
                                padding.toInt()
                            )
                        )
                    }

                    is CameraUpdate.Companion.CameraUpdateBasedOnPoint -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.newLatLng(
                                point.toMapLibreLatLng()
                            )
                        )
                    }

                    is CameraUpdate.Companion.CameraUpdateBasedOnPointAndBearing -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(point.toMapLibreLatLng())
                                    .bearing(bearing.toMapLibreBearing())
                                    .build()
                            )
                        )
                    }

                    is CameraUpdate.Companion.CameraUpdateBearing -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.bearingTo(
                                bearing.toMapLibreBearing()
                            )
                        )
                    }

                    is CameraUpdate.Companion.CameraUpdateTilt -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.tiltTo(
                                tilt.toMapLibreTilt()
                            )
                        )
                    }

                    is CameraUpdate.Companion.CameraUpdateZoom -> {
                        setOrEaseCamera(
                            CameraUpdateFactory.zoomTo(
                                zoom.toMapLibreZoom()
                            )
                        )
                    }
                }
            }
        }

    override fun addMarker(marker: Marker) = runWhenReady {
        if (markers.contains(marker.id)) {
            error("Marker with ID ${marker.id} already exists.")
        }
        var options = SymbolOptions()
        marker.point.let {
            options = options.withLatLng(it.toMapLibreLatLng())
        }
        marker.title?.let {
            options = options.withTextField(it)
        }
        marker.drawable?.let {
            val id = MAPLIBRE_SYMBOL_ID + marker.id
            if (style.getImage(id) == null) {
                style.addImage(
                    id,
                    AppCompatResources.getDrawable(viewInstance.context, it)!!.toBitmap()
                )
            }
            options = options.withIconImage(id)
        }
        marker.bitmap?.let {
            val id = MAPLIBRE_SYMBOL_ID + marker.id
            if (style.getImage(id) == null) {
                style.addImage(id, it)
            }
            options = options.withIconImage(id)
        }
        marker.scale.let {
            // HACK: Use 0.6x the size if drawable is used instead of bitmap.
            // Apparently the default marker size in MapLibre is larger compared to the one in Mapbox.
            options = options.withIconSize(
                it * (marker.drawable?.let { 0.6F } ?: 1.0F)
            )
        }
        marker.rotation.let {
            options = options.withIconRotate(it)
        }
        markers[marker.id] = symbolManager.create(options)
    }

    override fun addPolyline(polyline: Polyline) = runWhenReady {
        if (polylines.contains(polyline.id)) {
            error("Polyline with ID ${polyline.id} already exists.")
        }
        polylines.add(polyline.id)
        style.addSource(
            GeoJsonSource(
                MAPLIBRE_POLYLINE_SOURCE_ID + polyline.id,
                FeatureCollection.fromFeatures(
                    listOf(
                        Feature.fromGeometry(
                            LineString.fromLngLats(polyline.points.map { it.toMapLibrePoint() })
                        )
                    )
                ),
                GeoJsonOptions().withLineMetrics(true)
            )
        )
        var layer = LineLayer(
            MAPLIBRE_POLYLINE_LAYER_ID + polyline.id,
            MAPLIBRE_POLYLINE_SOURCE_ID + polyline.id
        )
            .withProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(polyline.width),
                lineColor(polyline.color),
                // No support for following attributes in MapLibre:
                // polyline.borderWidth
                // polyline.borderColor
            )
        if (polyline.colors != null) {
            layer = layer.withProperties(
                lineGradient(
                    interpolate(
                        linear(),
                        lineProgress(),
                        *polyline.colors.mapIndexed { index, color ->
                            Expression.stop(
                                (index + 1).toDouble() / polyline.points.size.toDouble(),
                                Expression.rgb(
                                    Color.red(color).toFloat(),
                                    Color.green(color).toFloat(),
                                    Color.blue(color).toFloat()
                                )
                            )
                        }.toTypedArray()
                    )
                )
            )
        }
        style.addLayerBelow(layer, MAPLIBRE_SYMBOL_LAYER_ID)
    }

    override fun addPolygon(polygon: Polygon) = runWhenReady {
        if (polygons.contains(polygon.id)) {
            error("Polygon with ID ${polygon.id} already exists.")
        }
        var options = FillOptions()
        polygon.points.let {
            options = options.withLatLngs(
                listOf(
                    it.map { point -> point.toMapLibreLatLng() }
                )
            )
        }
        polygon.color.let {
            options = options.withFillColor(it.toMapLibreColor())
        }
        polygon.opacity.let {
            options = options.withFillOpacity(it)
        }
        polygons[polygon.id] = fillManager.create(options)
    }

    override fun removeMarker(marker: Marker) = runWhenReady {
        if (!markers.contains(marker.id)) {
            error("Marker with ID ${marker.id} does not exist.")
        }
        markers.remove(marker.id)?.also {
            if (it.iconImage != null) {
                style.removeImage(it.iconImage!!)
            }
            symbolManager.delete(it)
        }
    }

    override fun removePolyline(polyline: Polyline) = runWhenReady {
        if (!polylines.contains(polyline.id)) {
            error("Polyline with ID ${polyline.id} does not exist.")
        }
        polylines.remove(polyline.id)
        style.removeSource(MAPLIBRE_POLYLINE_SOURCE_ID + polyline.id)
        style.removeLayer(MAPLIBRE_POLYLINE_LAYER_ID + polyline.id)
    }

    override fun removePolygon(polygon: Polygon) = runWhenReady {
        if (!polygons.contains(polygon.id)) {
            error("Polygon with ID ${polygon.id} does not exist.")
        }
        polygons.remove(polygon.id)?.also { fillManager.delete(it) }
    }

    override fun clearMarkers() = runWhenReady {
        markers.values.forEach {
            if (it.iconImage != null) {
                style.removeImage(it.iconImage!!)
            }
            symbolManager.delete(it)
        }
        markers.clear()
    }

    override fun clearPolylines() = runWhenReady {
        polylines.forEach {
            style.removeSource(MAPLIBRE_POLYLINE_SOURCE_ID + it)
            style.removeLayer(MAPLIBRE_POLYLINE_LAYER_ID + it)
        }
        polylines.clear()
    }

    override fun clearPolygons() = runWhenReady {
        polygons.values.forEach { fillManager.delete(it) }
        polygons.clear()
    }

    private fun Point.toMapLibrePoint() = org.maplibre.geojson.Point.fromLngLat(longitude, latitude)
    private fun Point.toMapLibreLatLng() = org.maplibre.android.geometry.LatLng(latitude, longitude)

    private fun org.maplibre.android.geometry.LatLng.toPoint() = Point(longitude, latitude)

    private fun Float.toMapLibreBearing() = this
        .times(MAPLIBRE_CAMERA_BEARING_MAX - MAPLIBRE_CAMERA_BEARING_MIN)
        .div(CAMERA_BEARING_MAX - CAMERA_BEARING_MIN)
        .toDouble()

    private fun Float.toMapLibreTilt() = this
        .times(MAPLIBRE_CAMERA_TILT_MAX - MAPLIBRE_CAMERA_TILT_MIN)
        .div(CAMERA_TILT_MAX - CAMERA_TILT_MIN)
        .toDouble()

    private fun Float.toMapLibreZoom() = this
        .times(MAPLIBRE_CAMERA_ZOOM_MAX - MAPLIBRE_CAMERA_ZOOM_MIN)
        .div(CAMERA_ZOOM_MAX - CAMERA_ZOOM_MIN)
        .toDouble()

    private fun Int.toMapLibreColor() = String.format("#%06X", 0xFFFFFF and this)


    companion object {
        internal const val MAPLIBRE_CAMERA_BEARING_MIN = 0.0F
        internal const val MAPLIBRE_CAMERA_BEARING_MAX = 360.0F
        internal const val MAPLIBRE_CAMERA_TILT_MIN = 0.0F
        internal const val MAPLIBRE_CAMERA_TILT_MAX = 60.0F
        internal const val MAPLIBRE_CAMERA_ZOOM_MIN = 0.0F
        internal const val MAPLIBRE_CAMERA_ZOOM_MAX = 22.0F

        internal const val MAPLIBRE_SYMBOL_ID = "symbol-"

        // https://github.com/maplibre/maplibre-plugins-android/blob/main/plugin-annotation/src/main/java/org/maplibre/android/plugins/annotation/SymbolElementProvider.java#L19
        internal const val MAPLIBRE_SYMBOL_LAYER_ID = "mapbox-android-symbol-layer-1"
        internal const val MAPLIBRE_POLYLINE_LAYER_ID = "polyline-layer-"
        internal const val MAPLIBRE_POLYLINE_SOURCE_ID = "polyline-source-"
    }
}
