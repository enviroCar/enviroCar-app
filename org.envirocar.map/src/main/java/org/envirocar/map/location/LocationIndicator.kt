package org.envirocar.map.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.envirocar.map.MapController
import org.envirocar.map.MapView

/**
 * [LocationIndicator]
 * -------------------
 * [LocationIndicator] allows to display the current location on the [MapView]. he class creates its
 * own internal [LocationManager] instance. The constructor takes existing [MapController] (bound to
 * a [MapView]) reference as a parameter.
 *
 * Following permissions should be granted to the application before instantiating this class:
 * * [Manifest.permission.ACCESS_FINE_LOCATION]
 * * [Manifest.permission.ACCESS_COARSE_LOCATION]
 */
class LocationIndicator(
    private val controller: MapController,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BaseLocationIndicator(controller, context, scope), LocationListener {
    private var location: Location? = null
    private var locationManager: LocationManager? = null

    /**
     * Enables the location indicator.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun enable() {
        super.enable()
        locationManager = context.getSystemService(LocationManager::class.java)
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0F,
            this@LocationIndicator
        )
        location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    /**
     * Disables the location indicator.
     */
    override fun disable() {
        super.disable()
        locationManager?.removeUpdates(this)
        location = null
        locationManager = null
    }

    override fun onLocationChanged(value: Location) {
        notifyLocation(value)
    }
}
