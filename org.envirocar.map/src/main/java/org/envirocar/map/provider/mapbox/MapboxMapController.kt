package org.envirocar.map.provider.mapbox

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.envirocar.map.MapController
import org.envirocar.map.R
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polyline

/**
 * [MapboxMapController]
 * ---------------------
 * [Mapbox](https://www.mapbox.com) based implementation for [MapController].
 */
class MapboxMapController(private val viewInstance: MapView) : MapController() {
    // https://docs.mapbox.com/android/maps/guides/annotations/annotations/
    // https://docs.mapbox.com/android/maps/examples/line-gradient/
    // NOTE: PolylineAnnotationManager API is not sufficient to create polylines with gradients.
    private val pointAnnotationManager = viewInstance.annotations.createPointAnnotationManager()

    private val scope = CoroutineScope(Dispatchers.Main)
    private val markers = mutableMapOf<Int, PointAnnotation>()
    private val polylines = mutableSetOf<Int>()
    private val styleLoadedCompletableDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    init {
        if (viewInstance.mapboxMap.style != null) {
            styleLoadedCompletableDeferred.complete(Unit)
        } else {
            viewInstance.mapboxMap.subscribeStyleLoaded {
                styleLoadedCompletableDeferred.complete(Unit)
            }
        }
    }

    override fun setMinZoom(minZoom: Float) {
        super.setMinZoom(minZoom)
        viewInstance.mapboxMap.setBounds(
            CameraBoundsOptions.Builder()
                .minZoom(minZoom.toMapboxZoom())
                .build()
        )
    }

    override fun setMaxZoom(maxZoom: Float) {
        super.setMaxZoom(maxZoom)
        viewInstance.mapboxMap.setBounds(
            CameraBoundsOptions.Builder()
                .maxZoom(maxZoom.toMapboxZoom())
                .build()
        )
    }

    override fun notifyCameraUpdate(cameraUpdate: CameraUpdate) {
        super.notifyCameraUpdate(cameraUpdate)
        with(cameraUpdate) {
            when (this) {
                is CameraUpdate.Companion.CameraUpdateBasedOnBounds -> {
                    viewInstance.mapboxMap.cameraForCoordinates(
                        points.map { it.toMapboxPoint() },
                        CameraOptions.Builder()
                            .padding(padding.toDouble().let { EdgeInsets(it, it, it, it) })
                            .build(),
                        null,
                        null,
                        null
                    ) {
                        viewInstance.mapboxMap.setCamera(it)
                    }
                }

                is CameraUpdate.Companion.CameraUpdateBasedOnPoint -> {
                    viewInstance.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(point.toMapboxPoint())
                            .build()
                    )
                }

                is CameraUpdate.Companion.CameraUpdateBearing -> {
                    viewInstance.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .bearing(bearing.toMapboxBearing())
                            .build()
                    )
                }

                is CameraUpdate.Companion.CameraUpdateTilt -> {
                    // Mapbox SDK refers to tilt as pitch.
                    viewInstance.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .pitch(tilt.toMapboxTilt())
                            .build()
                    )
                }

                is CameraUpdate.Companion.CameraUpdateZoom -> {
                    viewInstance.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .zoom(zoom.toMapboxZoom())
                            .build()
                    )
                }
            }
        }
    }

    override fun addMarker(marker: Marker) {
        if (markers.contains(marker.id)) {
            error("Marker with ID ${marker.id} already exists.")
        }
        var options = PointAnnotationOptions()
        marker.point.also {
            options = options.withPoint(it.toMapboxPoint())
        }
        marker.title?.also {
            options = options.withTextField(it)
        }
        // Mapbox does not include a default marker icon.
        (marker.drawable ?: R.drawable.marker_icon_default).also {
            options = options.withIconImage(
                AppCompatResources.getDrawable(viewInstance.context, it)!!.toBitmap()
            )
        }
        markers[marker.id] = pointAnnotationManager.create(options)
    }

    override fun addPolyline(polyline: Polyline) {
        if (polylines.contains(polyline.id)) {
            error("Polyline with ID ${polyline.id} already exists.")
        }
        scope.launch {
            styleLoadedCompletableDeferred.await()

            assert(viewInstance.mapboxMap.style != null) {
                "Mapbox style is not initialized."
            }
            polylines.add(polyline.id)
            viewInstance.mapboxMap.style?.addSource(
                geoJsonSource(MAPBOX_POLYLINE_SOURCE_ID + polyline.id) {
                    feature(Feature.fromGeometry(LineString.fromLngLats(polyline.points.map { it.toMapboxPoint() })))
                    lineMetrics(true)
                }
            )
            viewInstance.mapboxMap.style?.addLayer(
                lineLayer(
                    MAPBOX_POLYLINE_LAYER_ID + polyline.id,
                    MAPBOX_POLYLINE_SOURCE_ID + polyline.id
                ) {
                    lineCap(LineCap.ROUND)
                    lineJoin(LineJoin.ROUND)
                    lineWidth(polyline.width.toDouble())
                    lineColor(polyline.color)
                    lineBorderWidth(polyline.borderWidth.toDouble())
                    lineBorderColor(polyline.borderColor)
                    polyline.colors?.also {
                        lineGradient(
                            interpolate {
                                linear()
                                lineProgress()
                                for (i in polyline.points.indices) {
                                    stop {
                                        literal((i + 1).toDouble() / polyline.points.size.toDouble())
                                        color(it[i])
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    override fun removeMarker(marker: Marker) {
        if (!markers.contains(marker.id)) {
            error("Marker with ID ${marker.id} does not exist.")
        }
        markers.remove(marker.id)?.also { pointAnnotationManager.delete(it) }
    }

    override fun removePolyline(polyline: Polyline) {
        if (!polylines.contains(polyline.id)) {
            error("Polyline with ID ${polyline.id} does not exist.")
        }
        scope.launch {
            styleLoadedCompletableDeferred.await()

            assert(viewInstance.mapboxMap.style != null) {
                "Mapbox style is not initialized."
            }
            polylines.remove(polyline.id)
            viewInstance.mapboxMap.style?.removeStyleSource(MAPBOX_POLYLINE_SOURCE_ID + polyline.id)
            viewInstance.mapboxMap.style?.removeStyleLayer(MAPBOX_POLYLINE_LAYER_ID + polyline.id)
        }
    }

    private fun Point.toMapboxPoint() = com.mapbox.geojson.Point.fromLngLat(longitude, latitude)

    private fun Float.toMapboxBearing() = this
        .times(MAPBOX_CAMERA_BEARING_MAX - MAPBOX_CAMERA_BEARING_MIN)
        .div(CAMERA_BEARING_MAX - CAMERA_BEARING_MIN)
        .toDouble()

    private fun Float.toMapboxTilt() = this
        .times(MAPBOX_CAMERA_TILT_MAX - MAPBOX_CAMERA_TILT_MIN)
        .div(CAMERA_TILT_MAX - CAMERA_TILT_MIN)
        .toDouble()

    private fun Float.toMapboxZoom() = this
        .times(MAPBOX_CAMERA_ZOOM_MAX - MAPBOX_CAMERA_ZOOM_MIN)
        .div(CAMERA_ZOOM_MAX - CAMERA_ZOOM_MIN)
        .toDouble()


    companion object {
        internal const val MAPBOX_CAMERA_BEARING_MIN = 0.0F
        internal const val MAPBOX_CAMERA_BEARING_MAX = 360.0F
        internal const val MAPBOX_CAMERA_TILT_MIN = 0.0F
        internal const val MAPBOX_CAMERA_TILT_MAX = 60.0F
        internal const val MAPBOX_CAMERA_ZOOM_MIN = 0.0F
        internal const val MAPBOX_CAMERA_ZOOM_MAX = 22.0F

        internal const val MAPBOX_POLYLINE_LAYER_ID = "polyline-layer-"
        internal const val MAPBOX_POLYLINE_SOURCE_ID = "polyline-source-"
    }
}
