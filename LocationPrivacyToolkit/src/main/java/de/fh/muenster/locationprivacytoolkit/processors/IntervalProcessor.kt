package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.DurationFormat
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface

/**
 * The IntervalProcessor only relays locations in the configured frequency.
 * If available, previous locations are passed through.
 */
class IntervalProcessor(context: Context) : AbstractInternalLocationProcessor(context) {

    override val config = LocationPrivacyConfig.Interval
    override val titleId = R.string.intervalTitle
    override val subtitleId = R.string.intervalSubtitle
    override val descriptionId = R.string.intervalDescription
    override val userInterface = LocationProcessorUserInterface.Slider
    override val values = arrayOf(1800, 600, 60, 0)

    override fun formatLabel(value: Int): String =
        DurationFormat.humanReadableFormat(value.toLong(), config.sortIndex)

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