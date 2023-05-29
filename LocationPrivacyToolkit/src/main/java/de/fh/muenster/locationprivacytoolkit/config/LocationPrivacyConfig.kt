package de.fh.muenster.locationprivacytoolkit.config

import de.fh.muenster.locationprivacytoolkit.R

enum class LocationPrivacyConfig {
    Access,
    Accuracy,
    Interval,
    Visibility,
    AutoDeletion;

    val titleId: Int
        get() = when(this) {
            Access -> R.string.accessTitle
            Accuracy -> R.string.accuracyTitle
            Interval -> R.string.intervalTitle
            Visibility -> R.string.visibilityTitle
            AutoDeletion -> R.string.autoDeletionTitle
        }

    val subtitleId: Int
        get() = when(this) {
            Access -> R.string.accessSubtitle
            Accuracy -> R.string.accuracySubtitle
            Interval -> R.string.intervalSubtitle
            Visibility -> R.string.visibilitySubtitle
            AutoDeletion -> R.string.autoDeletionSubtitle
        }

    val descriptionId: Int
        get() = when(this) {
            Access -> R.string.accessDescription
            Accuracy -> R.string.accuracyDescription
            Interval -> R.string.intervalDescription
            Visibility -> R.string.visibilityDescription
            AutoDeletion -> R.string.autoDeletionDescription
        }

    val defaultValue: Int
        get() = when(this) {
            Access -> 0
            Accuracy -> 0
            Interval -> 0
            Visibility -> 0
            AutoDeletion -> 0
        }

    val values: Array<Int>
        get() = when(this) {
            Access -> arrayOf(0, 1)
            Accuracy -> arrayOf(1000, 500, 100, 0)
            Interval -> arrayOf(1000, 600, 60, 0)
            Visibility -> arrayOf(0, 1, 2, 3)
            AutoDeletion -> arrayOf(1000, 600, 60, 0)
        }

    val userInterface: LocationPrivacyConfigInterface
        get() = when(this) {
            Access -> LocationPrivacyConfigInterface.Switch
            Accuracy -> LocationPrivacyConfigInterface.Slider
            Interval -> LocationPrivacyConfigInterface.Slider
            Visibility -> LocationPrivacyConfigInterface.Slider
            AutoDeletion -> LocationPrivacyConfigInterface.Slider
        }

    val range: IntRange
        get() = IntRange(0, values.size - 1)

    fun formatLabel(value: Int): String {
        return when(this) {
            Access -> ""
            Accuracy -> "${value}m"
            Interval -> "${value}s"
            Visibility -> {
                when(value) {
                    1 -> "Friends"
                    2 -> "Contacts"
                    3 -> "Everyone"
                    else -> "None"
                }
            }
            AutoDeletion -> "${value}s"
        }
    }
    fun indexToValue(indexValue: Float): Int? {
        val configValues = this.values
        if (configValues.isNotEmpty()) {
            val index = indexValue.toInt()
            return configValues[index]
        }
        return null
    }

    fun valueToIndex(value: Int): Int? {
        val index = this.values.indexOf(value)
        if (index >= 0) {
            return index
        }
        return null
    }

}

enum class LocationPrivacyConfigInterface {
    Switch,
    Slider
}