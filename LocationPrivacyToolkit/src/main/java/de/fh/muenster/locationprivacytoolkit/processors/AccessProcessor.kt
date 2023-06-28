package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig

class AccessProcessor(context: Context): AbstractLocationProcessor(context) {
    override val configKey = LocationPrivacyConfig.Access

    override fun manipulateLocation(location: Location, config: Int): Location? {
        if (config == 1) {
            return location
        }
        return null
    }
}