package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.mapbox.mapboxsdk.geometry.LatLng
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.ui.ExclusionZoneProcessorFragment
import de.fh.muenster.locationprivacytoolkit.processors.ui.ExclusionZoneProcessorFragment.Companion.ACTION_EXCLUSION_ZONE_UPDATE
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ExclusionZone(val center: LatLng, val radiusMeters: Int)

/**
 * The ExclusionZoneProcessor contains a fragment, where users can define exclusion zones.
 * Locations within those zones are blocked.
 */
class ExclusionZoneProcessor(context: Context) : AbstractInternalLocationProcessor(context) {

    init {
        loadExclusionZones()
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter(ACTION_EXCLUSION_ZONE_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

    }

    private var exclusionZones: List<ExclusionZone>? = null

    override val config = LocationPrivacyConfig.ExclusionZone
    override val titleId = R.string.exclusionZoneTitle
    override val subtitleId = R.string.exclusionZoneSubtitle
    override val descriptionId = R.string.exclusionZoneDescription
    override val userInterface = LocationProcessorUserInterface.Fragment
    override val fragment: Fragment
        get() = ExclusionZoneProcessorFragment()

    override fun manipulateLocation(location: Location, config: Int): Location? {
        val zones = exclusionZones ?: return location

        if (zones.isNotEmpty()) {
            val locationLatLng = LatLng(location)
            val isExcluded = zones.any { z ->
                z.center.distanceTo(locationLatLng) < z.radiusMeters
            }
            if (isExcluded) {
                return null
            }
        }
        return location
    }

    private fun loadExclusionZones() {
        CoroutineScope(Dispatchers.IO).launch {
            locationPrivacyConfig.getPrivacyConfigString(LocationPrivacyConfig.ExclusionZone)
                ?.let { zonesJson ->
                    val zoneListType = object : TypeToken<List<ExclusionZone>>() {}.type
                    exclusionZones = try {
                        Gson().fromJson(zonesJson, zoneListType) as? List<ExclusionZone>
                    } catch (_: JsonSyntaxException) {
                        null
                    }
                }
        }
    }


    // BroadcastReceiver

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_EXCLUSION_ZONE_UPDATE) {
            loadExclusionZones()
        }
    }
}
