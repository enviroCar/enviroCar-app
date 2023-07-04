package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface

/**
 * The AccessProcessor passes through or blocks locations.
 */
class AccessProcessor(context: Context) : AbstractInternalLocationProcessor(context) {

    override val config = LocationPrivacyConfig.Access
    override val titleId = R.string.accessTitle
    override val subtitleId = R.string.accessSubtitle
    override val descriptionId = R.string.accessDescription
    override val userInterface = LocationProcessorUserInterface.Switch
    override val values = arrayOf(0, 1)

    override fun manipulateLocation(location: Location, config: Int): Location? {
        if (config == 1) {
            return location
        }
        return null
    }
}