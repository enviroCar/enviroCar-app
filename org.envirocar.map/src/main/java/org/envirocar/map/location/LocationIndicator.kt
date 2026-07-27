/**
 * Copyright (C) 2013 - 2026 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */

package org.envirocar.map.location

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
 * [LocationIndicator] allows to display the current location on the [MapView].
 *
 * The class uses its own internal [LocationManager] & [SensorManager].
 *
 * The constructor takes existing [MapController] (bound to a [MapView]) reference as a parameter.
 *
 * Following permissions should be granted to the application before instantiating this class:
 * * [Manifest.permission.ACCESS_FINE_LOCATION]
 * * [Manifest.permission.ACCESS_COARSE_LOCATION]
 */
class LocationIndicator(
    private val controller: MapController,
    private val context: Context
) : BaseLocationIndicator(controller, context), LocationListener, SensorEventListener {
    private val lock = Any()

    private var locationManager: LocationManager? = null

    private var sensorManager: SensorManager? = null
    private var sensorManagerAzimuth: Float? = null
    private var sensorManagerRotationMatrix = FloatArray(9)
    private var sensorManagerOrientation = FloatArray(3)
    private var accelerometerSensorEvent: SensorEvent? = null
    private var magneticFieldSensorEvent: SensorEvent? = null

    /**
     * Enables the location indicator.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun enable() {
        locationManager = context.getSystemService(LocationManager::class.java)
        sensorManager = context.getSystemService(SensorManager::class.java)
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            500L,
            0F,
            this@LocationIndicator
        )
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        super.enable()
    }

    /**
     * Disables the location indicator.
     */
    override fun disable() {
        locationManager?.removeUpdates(this)
        sensorManager?.unregisterListener(this)
        locationManager = null
        sensorManager = null
        super.disable()
    }

    override fun onLocationChanged(value: Location) = synchronized(lock) {
        // Set the bearing from the [Sensor.TYPE_MAGNETIC_FIELD] in the [Location], if available.
        value.bearing = sensorManagerAzimuth ?: value.bearing

        notifyLocation(value)
    }

    override fun onSensorChanged(event: SensorEvent?) = synchronized(lock) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) accelerometerSensorEvent = event
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) magneticFieldSensorEvent = event
        if (accelerometerSensorEvent != null && magneticFieldSensorEvent != null) {
            SensorManager.getRotationMatrix(
                sensorManagerRotationMatrix,
                null,
                accelerometerSensorEvent?.values,
                magneticFieldSensorEvent?.values
            )
            SensorManager.getOrientation(sensorManagerRotationMatrix, sensorManagerOrientation)
            sensorManagerAzimuth = (Math.toDegrees(sensorManagerOrientation[0].toDouble()) + 360).toFloat() % 360
            accelerometerSensorEvent = null
            magneticFieldSensorEvent = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
