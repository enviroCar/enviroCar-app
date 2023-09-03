package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.DurationFormat
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface

/**
 * The DelayProcessor holds back locations with a given delay
 * and returns viable previous locations if available.
 */
class DelayProcessor(context: Context) : AbstractInternalLocationProcessor(context) {

    override val config = LocationPrivacyConfig.Delay
    override val titleId = R.string.delayTitle
    override val subtitleId = R.string.delaySubtitle
    override val descriptionId = R.string.delayDescription
    override val userInterface = LocationProcessorUserInterface.Slider
    override val values = arrayOf(1800, 300, 60, 30, 10, 0)

    override fun formatLabel(value: Int): String =
        DurationFormat.humanReadableFormat(value.toLong(), config.sortIndex)

    private var previousLocations: MutableList<Location> = mutableListOf()

    override fun manipulateLocation(location: Location, config: Int): Location? {
        previousLocations.add(location)
        if (config > 0) {
            val currentTimeSeconds = System.currentTimeMillis() / 1000L
            val delayedLocation = previousLocations.lastOrNull { l ->
                val lTimeSeconds = l.time / 1000L
                (currentTimeSeconds - lTimeSeconds) >= config
            }
            return delayedLocation
        }
        return location
    }
}
