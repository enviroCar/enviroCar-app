package de.fh.muenster.locationprivacytoolkit

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.PendingIntent
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.*
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.processors.AccessProcessor
import de.fh.muenster.locationprivacytoolkit.processors.AccuracyProcessor
import de.fh.muenster.locationprivacytoolkit.processors.IntervalProcessor
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import java.util.function.Consumer

class LocationPrivacyToolkit(context: Context, private val listener: LocationPrivacyToolkitListener? = null): LocationListener {

    public val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    private var config = LocationPrivacyConfigManager(context)

    private val accessProcessor = AccessProcessor(context)
    private val accuracyProcessor = AccuracyProcessor(context)
    private val intervalProcessor = IntervalProcessor(context)

    private val internalListeners: MutableList<LocationListener> = mutableListOf()
    private val internalPendingIntents: MutableList<PendingIntent> = mutableListOf()

    private val autoDeletionTimeSeconds: Int?
        get() {
            val time = config.getPrivacyConfig(LocationPrivacyConfig.AutoDeletion) ?: return null
            return if (time <= 0) null else time
        }

    @RequiresApi(Build.VERSION_CODES.P)
    fun isLocationEnabled(): Boolean {
        return locationManager.isLocationEnabled
    }

    fun isProviderEnabled(provider: String): Boolean {
        return locationManager.isProviderEnabled(provider)
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun getLastKnownLocation(provider: String): Location? {
        val lastLocation = locationManager.getLastKnownLocation(provider) ?: return null
        return processLocation(lastLocation)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun getCurrentLocation(
        provider: String,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        consumer: Consumer<Location?>
    ) {
        val privacyConsumer = Consumer<Location> {
            val processedLocation = processLocation(it)
            consumer.accept(processedLocation)
        }
        locationManager.getCurrentLocation(provider, cancellationSignal, executor, privacyConsumer)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun getCurrentLocation(
        provider: String,
        locationRequest: LocationRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        consumer: Consumer<Location?>
    ) {
        val privacyConsumer = Consumer<Location> {
            val processedLocation = processLocation(it)
            consumer.accept(processedLocation)
        }
        locationManager.getCurrentLocation(provider, locationRequest, cancellationSignal, executor, privacyConsumer)
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        listener: LocationListener
    ) {
        internalListeners.add(listener)
        locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, this)
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        listener: LocationListener,
        looper: Looper?
    ) {
        internalListeners.add(listener)
        locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, this, looper)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        executor: Executor,
        listener: LocationListener
    ) {
        internalListeners.add(listener)
        locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, executor, this)
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        pendingIntent: PendingIntent
    ) {
        throw NotImplementedError("PendingIntents are not implemented yet")
        /*
        this.internalPendingIntents.add(pendingIntent)
        locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, pendingIntent)
        */
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        locationRequest: LocationRequest,
        executor: Executor,
        listener: LocationListener
    ) {
        internalListeners.add(listener)
        locationManager.requestLocationUpdates(provider, locationRequest, executor, this)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
        provider: String,
        locationRequest: LocationRequest,
        pendingIntent: PendingIntent
    ) {
        throw NotImplementedError("PendingIntents are not implemented yet")
        /*
        this.internalPendingIntents.add(pendingIntent)
        locationManager.requestLocationUpdates(provider, locationRequest, pendingIntent)
        */
    }

    fun removeUpdates(listener: LocationListener) {
        internalListeners.remove(listener)
        if (internalListeners.isEmpty()) {
            locationManager.removeUpdates(this)
        }
    }

    fun removeUpdates(pendingIntent: PendingIntent) {
        throw NotImplementedError("PendingIntents are not implemented yet")
        /*
        internalPendingIntents.remove(pendingIntent)
        if (internalPendingIntents.isEmpty()) {
            locationManager.removeUpdates(pendingIntent)
        }
        */
    }

    fun processLocation(location: Location?): Location? {
        // pipe location through all processors
        return location
                .let { accessProcessor.process(it) }
                .let { accuracyProcessor.process(it)}
                .let { intervalProcessor.process(it)}
    }

    // LocationListener

    override fun onLocationChanged(l: Location) {
        val processedLocation = processLocation(l) ?: return

        Log.i("location", "location: ${l.longitude}, ${l.latitude}")
        Log.i("location", "location: ${processedLocation.longitude}, ${processedLocation.latitude}")

        internalListeners.forEach { it.onLocationChanged(processedLocation) }
        internalPendingIntents.forEach { /* TODO */ }

        val autoDeletionTime = autoDeletionTimeSeconds ?: return
        MainScope().launch {
            delay(autoDeletionTime * 1000L)
            listener?.onRemoveLocation(processedLocation)
        }
    }
}

interface LocationPrivacyToolkitListener {
    fun onRemoveLocation(l: Location)
    fun onRemoveLocation(timestamp: Long)
    fun onRemoveLocationRange(fromTimestamp: Long, toTimestamp: Long)
}