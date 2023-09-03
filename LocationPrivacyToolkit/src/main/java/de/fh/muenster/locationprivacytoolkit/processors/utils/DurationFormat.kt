package de.fh.muenster.locationprivacytoolkit.processors.utils

import android.os.Build
import androidx.annotation.RequiresApi
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import java.time.Duration
import java.util.concurrent.TimeUnit

class DurationFormat {
    companion object {

        fun humanReadableFormat(timeSeconds: Long, index: Int): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                humanReadableFormatApi26(Duration.ofSeconds(timeSeconds), index)
            } else {
                "${timeSeconds}s"
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun humanReadableFormatApi26(duration: Duration, index: Int): String {
            val days = duration.toDays()
            val hours = duration.toHours() - TimeUnit.DAYS.toHours(days)
            val minutes = duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours())
            val seconds = duration.seconds - TimeUnit.MINUTES.toSeconds(duration.toMinutes())
            var durationString = ""
            if (days > 0) durationString += String.format("%sd", days)
            if (hours > 0) {
                if (durationString.isNotBlank()) durationString += " "
                durationString += String.format("%sh", hours)
            }
            if (minutes > 0) {
                if (durationString.isNotBlank()) durationString += " "
                durationString += String.format("%sm", minutes)
            }
            if (seconds > 0) {
                if (durationString.isNotBlank()) durationString += " "
                durationString += String.format("%ss", seconds)
            }
            if (durationString.isBlank()){
                if(index == LocationPrivacyConfig.Delay.sortIndex)
                    durationString = "No Delay"
                if(index == LocationPrivacyConfig.Interval.sortIndex)
                    durationString = "Frequently"
                if(index == LocationPrivacyConfig.AutoDeletion.sortIndex)
                    durationString = "No Deletion"
            }

            return durationString
        }
    }
}