package org.envirocar.map.provider.mapbox

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import org.envirocar.map.MapController
import org.envirocar.map.R
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Polyline

/**
 * [MapboxMapController]
 * -------------------
 * [Mapbox](https://www.mapbox.com) based implementation for [MapController].
 */
class MapboxMapController(private val viewInstance: MapView) : MapController() {
    // https://docs.mapbox.com/android/maps/guides/annotations/annotations/
    private val pointAnnotationManager = viewInstance.annotations.createPointAnnotationManager()
    private val polylineAnnotationManager = viewInstance.annotations.createPolylineAnnotationManager()

    private val markers = mutableMapOf<Int, PointAnnotation>()
    private val polylines = mutableMapOf<Int, PolylineAnnotation>()

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
                        points.map { Point.fromLngLat(it.longitude, it.latitude) },
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
                            .center(Point.fromLngLat(point.longitude, point.latitude))
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
            options = options.withPoint(Point.fromLngLat(it.longitude, it.latitude))
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
        // TODO: Missing implementation.
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
        // TODO: Missing implementation.
    }

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
    }
}
