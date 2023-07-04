package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.Location
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationPrivacyVisibility
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface

/**
 * The VisibilityProcessor enables the user to configure, who to share their location with.
 * The actual handling of this setting needs to be handled by the implementing app.
 */
class VisibilityProcessor(private val context: Context, listener: LocationPrivacyToolkitListener?) :
    AbstractInternalLocationProcessor(context, listener) {

    override val config = LocationPrivacyConfig.Visibility
    override val titleId = R.string.visibilityTitle
    override val subtitleId = R.string.visibilitySubtitle
    override val descriptionId = R.string.visibilityDescription
    override val userInterface = LocationProcessorUserInterface.Slider
    override val values = LocationPrivacyVisibility.values().map { v -> v.value }.toTypedArray()

    override fun formatLabel(value: Int): String {
        return when (LocationPrivacyVisibility.fromInt(value)) {
            LocationPrivacyVisibility.Nobody -> context.getString(R.string.visibilityNoneOption)
            LocationPrivacyVisibility.Friends -> context.getString(R.string.visibilityFriendsOption)
            LocationPrivacyVisibility.Contacts -> context.getString(R.string.visibilityContactsOption)
            LocationPrivacyVisibility.Everybody -> context.getString(R.string.visibilityEveryoneOption)
        }
    }

    override fun manipulateLocation(location: Location, config: Int): Location {
        // no need to change the location
        return location
    }
}