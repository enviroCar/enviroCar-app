package org.envirocar.map

import androidx.annotation.CallSuper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.envirocar.map.camera.CameraState
import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.camera.CameraUpdateFactory
import org.envirocar.map.model.Animation
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polygon
import org.envirocar.map.model.Polyline

/**
 * [MapController]
 * ---------------
 * [MapController] provides various methods to interact with the visible [MapView].
 *
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     val view: MapView = findViewById(R.id.mapView)
 *     val controller: MapController = mapView.getController(...)
 *     /* Interact with the [MapView] using the [MapController]. */
 * }
 * ```
 *
 * @see MapView
 * @see CameraUpdate
 * @see CameraUpdateFactory
 * @see Animation
 * @see Marker
 * @see Point
 * @see Polygon
 * @see Polyline
 */
abstract class MapController {
    private var jobLock = Any()
    private var queueLock = Any()
    private var job: Job? = null
    private val queue = mutableListOf<() -> Unit>()
    internal val scope = CoroutineScope(Dispatchers.Main)
    internal val readyCompletableDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    /** [CameraState] provides access to various camera attributes as [StateFlow]. */
    abstract val camera: CameraState

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
    open fun notifyCameraUpdate(cameraUpdate: CameraUpdate, animation: Animation? = null) {
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

    /** Adds a [Polygon] from the [MapView]. */
    abstract fun addPolygon(polygon: Polygon)

    /** Removes a [Marker] from the [MapView]. */
    abstract fun removeMarker(marker: Marker)

    /** Removes a [Polyline] from the [MapView]. */
    abstract fun removePolyline(polyline: Polyline)

    /** Removes a [Polygon] from the [MapView]. */
    abstract fun removePolygon(polygon: Polygon)

    /** Removes all [Marker]s from the [MapView]. */
    abstract fun clearMarkers()

    /** Removes all [Polyline]s from the [MapView]. */
    abstract fun clearPolylines()

    /** Removes all [Polygon]s from the [MapView]. */
    abstract fun clearPolygons()

    /**
     * Executes the specified [block] once [readyCompletableDeferred] is completed.
     * The order of execution for subsequent calls is preserved.
     */
    internal fun runWhenReady(block: () -> Unit) {
        synchronized(jobLock) {
            synchronized(queueLock) { queue.add(block) }
            if (job == null) {
                job = scope.launch {
                    readyCompletableDeferred.await()
                    while (synchronized(queueLock) { queue.isNotEmpty() }) {
                        synchronized(queueLock) { queue.removeAt(0).invoke() }
                    }
                    synchronized(jobLock) { job = null }
                }
            }
        }
    }

    companion object {
        internal const val CAMERA_BEARING_MIN = 0.0F
        internal const val CAMERA_BEARING_MAX = 360.0F
        internal const val CAMERA_TILT_MIN = 0.0F
        internal const val CAMERA_TILT_MAX = 60.0F
        internal const val CAMERA_ZOOM_MIN = 0.0F
        internal const val CAMERA_ZOOM_MAX = 22.0F
    }
}
