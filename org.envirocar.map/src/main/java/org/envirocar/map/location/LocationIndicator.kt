package org.envirocar.map.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.envirocar.map.MapController
import org.envirocar.map.MapView
import org.envirocar.map.camera.CameraState
import org.envirocar.map.camera.CameraUpdateFactory
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polygon

/**
 * [LocationIndicator]
 * -------------------
 * [LocationIndicator] allows to display the current location on the [MapView].
 * The constructor takes existing [MapController] (bound to a [MapView]) reference as a parameter.
 *
 * Following permissions should be granted to the application before instantiating this class:
 * * [Manifest.permission.ACCESS_FINE_LOCATION]
 * * [Manifest.permission.ACCESS_COARSE_LOCATION]
 */
class LocationIndicator(
    private val controller: MapController,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : LocationListener {
    private val markers = mutableListOf<Marker>()
    private val polygons = mutableListOf<Polygon>()

    private var lock = Any()
    private var enabled = false
    private var location: Location? = null
    private var locationManager: LocationManager? = null
    private var locationIndicatorCameraMode: LocationIndicatorCameraMode = LocationIndicatorCameraMode.None
    private var followCameraDebounceJob: Job? = null

    /**
     * Enables the location indicator.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun enable() {
        if (enabled) {
            error("LocationIndicator is already enabled.")
        }
        enabled = true
        locationManager = context.getSystemService(LocationManager::class.java)
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0F,
            this@LocationIndicator
        )
        location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        notifyUpdate()
    }

    /**
     * Disables the location indicator.
     */
    fun disable() {
        if (!enabled) {
            error("LocationIndicator is already disabled.")
        }
        enabled = false
        locationManager?.removeUpdates(this)
        location = null
        locationManager = null
        clearMarkers()
        clearPolygons()
    }

    /**
     * Sets the camera mode.
     */
    fun setCameraMode(value: LocationIndicatorCameraMode) {
        locationIndicatorCameraMode = value
        location?.let { followCameraIfRequired(it.toPoint()) }
    }

    private fun notifyUpdate() {
        location?.let {
            if (enabled) {
                clearMarkers()
                clearPolygons()

                markers.add(LocationPointMarker(it.toPoint(), context))
                if (it.hasBearing()) {
                    markers.add(
                        LocationBearingMarker(
                            it.toPoint(),
                            it.bearing - controller.camera.bearing.value,
                            context
                        )
                    )
                }
                if (it.hasAccuracy()) {
                    polygons.add(
                        LocationAccuracyPolygon(
                            it.toPoint(),
                            it.accuracy
                        )
                    )
                }

                markers.forEach { marker -> controller.addMarker(marker) }
                polygons.forEach { polygon -> controller.addPolygon(polygon) }

                followCameraIfRequired(it.toPoint())
            }
        }
    }

    private fun followCameraIfRequired(point: Point) {
        locationIndicatorCameraMode.let { value ->
            if (value is LocationIndicatorCameraMode.Follow) {
                followCameraDebounceJob?.cancel()
                followCameraDebounceJob = scope.launch {
                    delay(FOLLOW_CAMERA_DEBOUNCE_DELAY)
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

    /**
     * Invoked whenever the location related markers & polygons are required to be updated:
     * * Due to location updates from [LocationManager].
     * * Due to camera updates from [CameraState].
     */
    override fun onLocationChanged(value: Location) {
        location = value
        synchronized(lock) { notifyUpdate() }
    }

    init {
        controller.camera.bearing.onEach { synchronized(lock) { notifyUpdate() } }.launchIn(scope)
    }


    private fun Location.toPoint() = Point(latitude, longitude)

    companion object {
        private const val FOLLOW_CAMERA_DEBOUNCE_DELAY = 200L
    }
}
