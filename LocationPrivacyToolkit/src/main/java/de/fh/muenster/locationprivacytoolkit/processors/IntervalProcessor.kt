package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig

class IntervalProcessor(context: Context): AbstractLocationProcessor(context) {
    override val configKey = LocationPrivacyConfig.Interval

    private var localLastLocation: Location? = null
    private var lastLocation: Location?
        get() = localLastLocation ?: locationPrivacyConfig.getLastLocation().also {
            localLastLocation = it
        }
        set(value) {
            localLastLocation = value
            locationPrivacyConfig.setLastLocation(value)
        }

    override fun manipulateLocation(location: Location, config: Int): Location? {
        val cachedLocation = lastLocation
        if (config > 0 && cachedLocation != null && cachedLocation.time > 0) {
            val timeDiffToLastLocation = location.time - cachedLocation.time
            if (timeDiffToLastLocation < config * 1000) {
                return cachedLocation
            }
        }
        lastLocation = location
        return location
    }
}