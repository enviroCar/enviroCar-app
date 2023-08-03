package de.fh.muenster.locationprivacytoolkit

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.PendingIntent
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.*
import android.os.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.processors.AbstractInternalLocationProcessor
import de.fh.muenster.locationprivacytoolkit.processors.AbstractExternalLocationProcessor
import de.fh.muenster.locationprivacytoolkit.processors.db.LocationDatabase
import de.fh.muenster.locationprivacytoolkit.processors.db.LocationPrivacyDatabase
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationPrivacyVisibility
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import java.util.function.Consumer

    class LocationPrivacyToolkit(
        context: Context
    ) : LocationListener, LocationPrivacyToolkitListener {

        init {
            internalProcessors.clear()

            internalProcessors.addAll(
                LocationPrivacyConfigManager.getLocationProcessors(context, this)
            )

        }

        public val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        private val internalListeners: MutableList<LocationListener> = mutableListOf()
        private val internalPendingIntents: MutableList<PendingIntent> = mutableListOf()
        private val locationDatabase: LocationPrivacyDatabase by lazy {
            LocationPrivacyDatabase.sharedInstance(context)
        }

        fun loadAllLocations(): List<Location> {
            return locationDatabase.loadLocations()
        }

        fun loadLocations(fromTimestamp: Long, toTimestamp: Long): List<Location> {
            return locationDatabase.loadLocations(false, fromTimestamp, toTimestamp)
        }

        fun loadLocations(atTimestamp: Long): Location {
            return locationDatabase.loadLocation(false, atTimestamp)
        }

        // LocationManager methods

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
            locationManager.getCurrentLocation(
                provider, locationRequest, cancellationSignal, executor, privacyConsumer
            )
        }

        @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
        fun requestLocationUpdates(
            provider: String, minTimeMs: Long, minDistanceM: Float, listener: LocationListener
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
            provider: String, minTimeMs: Long, minDistanceM: Float, pendingIntent: PendingIntent
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
            provider: String, locationRequest: LocationRequest, pendingIntent: PendingIntent
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
            var l = location
            internalProcessors.forEach { p -> l = p.process(l) }
            externalProcessors.forEach { p -> l = p.process(l) }
            return l
        }

        // LocationListener

        override fun onLocationChanged(l: Location) {
            val processedLocation = processLocation(l) ?: return
            internalListeners.forEach { it.onLocationChanged(processedLocation) }
            internalPendingIntents.forEach { /* TODO */ }
        }

        companion object {
            internal val internalProcessors: MutableList<AbstractInternalLocationProcessor> =
                mutableListOf()
            var externalProcessors: MutableList<AbstractExternalLocationProcessor> = mutableListOf()
            var mapTilesUrl: String = "https://demotiles.maplibre.org/style.json"
        }

        override fun onRemoveLocation(l: Location) {
            TODO("Not yet implemented")
        }

        override fun onRemoveLocation(timestamp: Long) {
            TODO("Not yet implemented")
        }

        override fun onRemoveLocations(locations: List<Location>) {
            TODO("Not yet implemented")
        }

        override fun onRemoveLocationRange(fromTimestamp: Long, toTimestamp: Long) {
            TODO("Not yet implemented")
        }

        override fun onUpdateVisibilityPreference(visibility: LocationPrivacyVisibility) {
            TODO("Not yet implemented")
        }
    }

    interface LocationPrivacyToolkitListener {
        fun onRemoveLocation(l: Location)
        fun onRemoveLocation(timestamp: Long)
        fun onRemoveLocations(locations: List<Location>)
        fun onRemoveLocationRange(fromTimestamp: Long, toTimestamp: Long)
        fun onUpdateVisibilityPreference(visibility: LocationPrivacyVisibility)
    }