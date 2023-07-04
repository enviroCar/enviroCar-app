package de.fh.muenster.locationprivacytoolkit.processors.utils

enum class LocationPrivacyVisibility(val value: Int) {
    Nobody(3), Friends(2), Contacts(1), Everybody(0);

    companion object {
        fun fromInt(value: Int) =
            LocationPrivacyVisibility.values().first { it.value == value }
    }
}