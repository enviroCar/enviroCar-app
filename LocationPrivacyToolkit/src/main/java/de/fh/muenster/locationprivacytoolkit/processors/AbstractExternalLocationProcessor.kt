package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.Location
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig

abstract class AbstractExternalLocationProcessor(
    context: Context, listener: LocationPrivacyToolkitListener? = null
) : AbstractInternalLocationProcessor(context, listener) {

    // use an unique config-key to differentiate between various processors
    abstract val configKey: String
    final override val config = LocationPrivacyConfig.External

    override fun manipulateLocation(location: Location, config: Int): Location? {
        // override in your app
        TODO("implement manipulateLocation() in your processor")
    }
}