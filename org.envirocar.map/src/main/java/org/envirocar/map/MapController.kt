package org.envirocar.map

import androidx.annotation.CallSuper
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Polyline

/**
 * [MapController]
 * ---------------
 * [MapController] provides various methods to interact with the visible [MapView].
 *
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     val mapView: MapView = findViewById(R.id.mapView)
 *     val mapController: MapController = mapView.getController(...)
 *     /* Interact with the [MapView] using the [MapController]. */
 * }
 * ```
 *
 * @see MapView
 * @see CameraUpdate
 * @see Marker
 * @see Polyline
 */
abstract class MapController {
    /** Sets the minimum zoom level. */
    @CallSuper
    open fun setMinZoom(minZoom: Float) {
        assert(minZoom in CAMERA_ZOOM_MIN..CAMERA_ZOOM_MAX) {
            "Minimum zoom level must be between $CAMERA_ZOOM_MIN and $CAMERA_ZOOM_MAX."
        }
    }

    /** Sets the maximum zoom level. */
    @CallSuper
    open fun setMaxZoom(maxZoom: Float) {
        assert(maxZoom in CAMERA_ZOOM_MIN..CAMERA_ZOOM_MAX) {
            "Maximum zoom level must be between $CAMERA_ZOOM_MIN and $CAMERA_ZOOM_MAX."
        }
    }

    /** Notifies the [MapView] about a [CameraUpdate]. */
    @CallSuper
    open fun notifyCameraUpdate(cameraUpdate: CameraUpdate) {
        with(cameraUpdate) {
            when (this) {
                is CameraUpdate.Companion.CameraUpdateBearing -> {
                    assert(bearing in CAMERA_BEARING_MIN..CAMERA_BEARING_MAX) {
                        "Bearing must be between $CAMERA_BEARING_MIN and $CAMERA_BEARING_MAX."
                    }
                }

                is CameraUpdate.Companion.CameraUpdateTilt -> {
                    assert(tilt in CAMERA_TILT_MIN..CAMERA_TILT_MAX) {
                        "Tilt must be between $CAMERA_TILT_MIN and $CAMERA_TILT_MAX."
                    }
                }

                is CameraUpdate.Companion.CameraUpdateZoom -> {
                    assert(zoom in CAMERA_ZOOM_MIN..CAMERA_ZOOM_MAX) {
                        "Zoom must be between $CAMERA_ZOOM_MIN and $CAMERA_ZOOM_MAX."
                    }
                }

                else -> {
                    /* NO/OP */
                }
            }
        }
    }

    /** Adds a [Marker] to the [MapView]. */
    abstract fun addMarker(marker: Marker)

    /** Adds a [Polyline] to the [MapView]. */
    abstract fun addPolyline(polyline: Polyline)

    /** Removes a [Marker] from the [MapView]. */
    abstract fun removeMarker(marker: Marker)

    /** Removes a [Polyline] from the [MapView]. */
    abstract fun removePolyline(polyline: Polyline)

    companion object {
        internal const val CAMERA_BEARING_MIN = 0.0F
        internal const val CAMERA_BEARING_MAX = 360.0F
        internal const val CAMERA_TILT_MIN = 0.0F
        internal const val CAMERA_TILT_MAX = 60.0F
        internal const val CAMERA_ZOOM_MIN = 0.0F
        internal const val CAMERA_ZOOM_MAX = 22.0F
    }
}
