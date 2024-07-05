package org.envirocar.map.location

import android.content.Context
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.envirocar.map.MapController
import org.envirocar.map.MapView
import org.envirocar.map.camera.CameraUpdateFactory
import org.envirocar.map.location.annotation.LocationAccuracyPolygon
import org.envirocar.map.location.annotation.LocationBearingMarker
import org.envirocar.map.location.annotation.LocationPointMarker
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polygon

/**
 * [BaseLocationIndicator]
 * -----------------------
 * [BaseLocationIndicator] allows to display the provided location on the [MapView]. The current
 * user location may be supplied manually using the [notifyLocation] method of the instance. The
 * constructor takes existing [MapController] (bound to a [MapView]) reference as a parameter.
 */
open class BaseLocationIndicator(
    private val controller: MapController,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val markers = mutableListOf<Marker>()
    private val polygons = mutableListOf<Polygon>()

    private var lock = Any()
    private var enabled = false
    private var location: Location? = null
    private var locationIndicatorCameraMode: LocationIndicatorCameraMode = LocationIndicatorCameraMode.None
    private var followCameraDebounceJob: Job? = null

    /**
     * Enables the location indicator.
     */
    open fun enable() {
        if (enabled) {
            error("LocationIndicator is already enabled.")
        }
        enabled = true
        location?.let { notifyLocation(it) }
    }

    /**
     * Disables the location indicator.
     */
    open fun disable() {
        if (!enabled) {
            error("LocationIndicator is already disabled.")
        }
        enabled = false
        location = null
        clearMarkers()
        clearPolygons()
    }

    /**
     * Sets the camera mode.
     */
    fun setCameraMode(value: LocationIndicatorCameraMode) {
        locationIndicatorCameraMode = value
        location?.let { followCameraIfRequired(it) }
    }

    /**
     * Notifies about the current user location to update the [MapView].
     */
    fun notifyLocation(location: Location) = synchronized(lock) {
        if (enabled) {
            clearMarkers()
            clearPolygons()

            markers.add(LocationPointMarker(location.toPoint(), context))
            if (location.hasBearing()) {
                markers.add(
                    LocationBearingMarker(
                        location.toPoint(),
                        location.bearing - controller.camera.bearing.value,
                        context
                    )
                )
            }
            if (location.hasAccuracy()) {
                polygons.add(
                    LocationAccuracyPolygon(
                        location.toPoint(),
                        location.accuracy
                    )
                )
            }

            markers.forEach { marker -> controller.addMarker(marker) }
            polygons.forEach { polygon -> controller.addPolygon(polygon) }

            followCameraIfRequired(location)
        }
    }

    private fun followCameraIfRequired(location: Location) {
        locationIndicatorCameraMode.let { value ->
            if (value is LocationIndicatorCameraMode.Follow) {
                followCameraDebounceJob?.cancel()
                followCameraDebounceJob = scope.launch {
                    delay(FOLLOW_CAMERA_DEBOUNCE_DELAY)
                    val point = location.toPoint()
                    controller.notifyCameraUpdate(
                        CameraUpdateFactory.newCameraUpdateBasedOnPoint(point),
                        animation = value.animation
                    )
                }
            }
        }
    }

    private fun clearMarkers() {
        markers.forEach { controller.removeMarker(it) }
        markers.clear()
    }

    private fun clearPolygons() {
        polygons.forEach { controller.removePolygon(it) }
        polygons.clear()
    }

    init {
        controller.camera.bearing
            .onEach { synchronized(lock) { location?.let { notifyLocation(it) } } }
            .launchIn(scope)
    }

    private fun Location.toPoint() = Point(latitude, longitude)

    companion object {
        private const val FOLLOW_CAMERA_DEBOUNCE_DELAY = 200L
    }
}
