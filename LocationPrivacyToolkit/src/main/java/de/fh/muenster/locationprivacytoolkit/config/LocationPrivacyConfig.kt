package de.fh.muenster.locationprivacytoolkit.config

import android.content.Context
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.processors.AbstractInternalLocationProcessor
import de.fh.muenster.locationprivacytoolkit.processors.AccessProcessor
import de.fh.muenster.locationprivacytoolkit.processors.AccuracyProcessor
import de.fh.muenster.locationprivacytoolkit.processors.AutoDeletionProcessor
import de.fh.muenster.locationprivacytoolkit.processors.DelayProcessor
import de.fh.muenster.locationprivacytoolkit.processors.ExclusionZoneProcessor
import de.fh.muenster.locationprivacytoolkit.processors.HistoryProcessor
import de.fh.muenster.locationprivacytoolkit.processors.IntervalProcessor
import de.fh.muenster.locationprivacytoolkit.processors.VisibilityProcessor

enum class LocationPrivacyConfig {
    Access,
    History,
    ExclusionZone,
    Accuracy,
    AutoDeletion,
    Delay,
    Interval,
    Visibility,
    External;

    val sortIndex: Int
        get() = when (this) {
            Access -> 0
            ExclusionZone -> 1
            Accuracy -> 2
            Delay -> 3
            Interval -> 4
            AutoDeletion -> 5
            Visibility -> 6
            History -> 7
            External -> Int.MAX_VALUE
        }

    fun getLocationProcessor(
        context: Context,
        listener: LocationPrivacyToolkitListener?
    ): AbstractInternalLocationProcessor? {
        return when (this) {
            Access -> AccessProcessor(context)
            Accuracy -> AccuracyProcessor(context)
            Delay -> DelayProcessor(context)
            ExclusionZone -> ExclusionZoneProcessor(context)
            Interval -> IntervalProcessor(context)
            Visibility -> VisibilityProcessor(context, listener)
            AutoDeletion -> AutoDeletionProcessor(context, listener)
            History -> HistoryProcessor(context, listener)
            else -> null
        }
    }

}