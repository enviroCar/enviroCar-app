package org.envirocar.map.provider.mapbox

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import org.envirocar.map.MapController
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Polyline

/**
 * [MapboxMapController]
 * -------------------
 * [Mapbox](https://www.mapbox.com) based implementation for [MapController].
 */
class MapboxMapController(private val viewInstance: MapView) : MapController() {

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
                        CameraOptions.Builder().build(),
                        padding.toDouble().let { EdgeInsets(it, it, it, it) },
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
                    // NOTE: Mapbox SDK refers to tilt as pitch.
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
        TODO("Not yet implemented")
    }

    override fun addPolyline(polyline: Polyline) {
        TODO("Not yet implemented")
    }

    override fun removeMarker(marker: Marker) {
        TODO("Not yet implemented")
    }

    override fun removePolyline(polyline: Polyline) {
        TODO("Not yet implemented")
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
