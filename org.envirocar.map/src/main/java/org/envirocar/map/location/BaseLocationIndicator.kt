package org.envirocar.map.location

import android.content.Context
import android.location.Location
import androidx.annotation.CallSuper
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
    internal var location: Location? = null

    private val markers = mutableListOf<Marker>()
    private val polygons = mutableListOf<Polygon>()

    private val lock = Any()
    private var enabled = false
    private var locationIndicatorCameraMode: LocationIndicatorCameraMode = LocationIndicatorCameraMode.None
    private var followCameraDebounceJob: Job? = null

    /**
     * Enables the location indicator.
     */
    @CallSuper
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
    @CallSuper
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
    fun notifyLocation(value: Location) = synchronized(lock) {
        location = value
        if (enabled) {

            clearMarkers()
            clearPolygons()

            markers.add(LocationPointMarker(value.toPoint(), context))
            if (value.hasBearing()) {
                markers.add(
                    LocationBearingMarker(
                        value.toPoint(),
                        value.bearing - controller.camera.bearing.value,
                        context
                    )
                )
            }
            if (value.hasAccuracy()) {
                polygons.add(
                    LocationAccuracyPolygon(
                        value.toPoint(),
                        value.accuracy
                    )
                )
            }

            markers.forEach { marker -> controller.addMarker(marker) }
            polygons.forEach { polygon -> controller.addPolygon(polygon) }

            followCameraIfRequired(value)
        }
    }

    private fun followCameraIfRequired(location: Location) {
        locationIndicatorCameraMode.let { value ->
            if (value is LocationIndicatorCameraMode.Follow) {
                if (followCameraDebounceJob?.isActive != true) {
                    followCameraDebounceJob = scope.launch {
                        val cameraUpdate = if (location.hasBearing()) {
                            CameraUpdateFactory.newCameraUpdateBasedOnPointAndBearing(
                                location.toPoint(),
                                location.bearing,
                            )
                        } else {
                            CameraUpdateFactory.newCameraUpdateBasedOnPoint(
                                location.toPoint()
                            )
                        }

                        controller.notifyCameraUpdate(
                            cameraUpdate,
                            animation = value.animation
                        )
                        delay(value.animation.duration)
                    }
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

}
