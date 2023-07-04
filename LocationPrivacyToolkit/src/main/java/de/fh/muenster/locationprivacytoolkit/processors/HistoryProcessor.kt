package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.Location
import androidx.fragment.app.Fragment
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.db.LocationPrivacyDatabase
import de.fh.muenster.locationprivacytoolkit.processors.ui.HistoryProcessorFragment
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The HistoryProcessor collects all locations after they are processed and stores them in a database.
 * With the contained fragment, users are enabled to explore those locations and delete them.
 */
class HistoryProcessor(context: Context, listener: LocationPrivacyToolkitListener?) :
    AbstractInternalLocationProcessor(context, listener) {

    override val config = LocationPrivacyConfig.History
    override val titleId = R.string.historyTitle
    override val subtitleId = R.string.historySubtitle
    override val descriptionId = R.string.historyDescription
    override val userInterface = LocationProcessorUserInterface.Fragment

    override val fragment: Fragment
        get() = HistoryProcessorFragment()

    private val locationDatabase = LocationPrivacyDatabase.sharedInstance(context)

    override fun manipulateLocation(location: Location, config: Int): Location {
        CoroutineScope(Dispatchers.IO).launch {
            locationDatabase.add(location)
        }
        return location
    }
}