package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import android.os.Build
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.DurationFormat
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The AutoDeletionProcessor changes deletes location after a specified time.
 */
class AutoDeletionProcessor(context: Context, listener: LocationPrivacyToolkitListener?) :
    AbstractInternalLocationProcessor(context, listener) {

    override val config = LocationPrivacyConfig.AutoDeletion
    override val titleId = R.string.autoDeletionTitle
    override val subtitleId = R.string.autoDeletionSubtitle
    override val descriptionId = R.string.autoDeletionDescription
    override val userInterface = LocationProcessorUserInterface.Slider
    override val values = arrayOf(1800, 600, 60, 10, 0)

    override fun formatLabel(value: Int): String =
        DurationFormat.humanReadableFormat(value.toLong())

    override fun manipulateLocation(location: Location, config: Int): Location {
        if (config > 0) {
            val autoDeletionTime = config * 1000L
            MainScope().launch {
                delay(autoDeletionTime)
                // callback to delete
                listener?.onRemoveLocation(location)
            }
        }
        return location
    }
}