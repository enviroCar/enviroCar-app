package de.fh.muenster.locationprivacytoolkit.config

import android.content.Context
import android.location.Location
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.processors.AbstractInternalLocationProcessor
import java.lang.Exception

class LocationPrivacyConfigManager(context: Context) {

    private val preferences =
        context.getSharedPreferences(LOCATION_PRIVACY_PREFERENCES, Context.MODE_PRIVATE)

    fun getPrivacyConfig(config: LocationPrivacyConfig): Int? {
        return getPrivacyConfig(config.name)
    }
    fun getPrivacyConfig(key: String): Int? {
        return if (preferences.contains(key)) {
            try {
                preferences.getInt(key, -1)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getPrivacyConfigString(config: LocationPrivacyConfig): String? {
        return getPrivacyConfigString(config.name)
    }

    fun getPrivacyConfigString(key: String): String? {
        return if (preferences.contains(key)) {
            preferences.getString(key, "")
        } else {
            null
        }
    }

    fun setPrivacyConfig(config: LocationPrivacyConfig, value: Int) {
        setPrivacyConfig(config.name, value)
    }

    fun setPrivacyConfig(key: String, value: Int) {
        preferences.edit(commit = true) { putInt(key, value) }
    }

    fun setPrivacyConfig(key: LocationPrivacyConfig, value: String) {
        preferences.edit(commit = true) { putString(key.name, value) }
    }
    fun removePrivacyConfig(config: LocationPrivacyConfig) {
        removePrivacyConfig(config.name)
    }

    fun removePrivacyConfig(key: String) {
        preferences.edit(commit = true) { remove(key) }
    }

    fun getLastLocation(): Location? {
        if (preferences.contains(LAST_LOCATION_KEY)) {
            val jsonLocation = preferences.getString(LAST_LOCATION_KEY, null)
            val location: Location? = try {
                Gson().fromJson(jsonLocation, Location::class.java)
            } catch (_: JsonSyntaxException) {
                null
            }
            return location
        }
        return null
    }

    fun setLastLocation(location: Location?) {
        val jsonLocation = Gson().toJson(location)
        preferences.edit(commit = true) { putString(LAST_LOCATION_KEY, jsonLocation) }
    }

    fun setUseExampleData(useExampleData: Boolean) {
        preferences.edit(commit = true) { putBoolean(USE_EXAMPLE_DATA_KEY, useExampleData) }
    }

    fun getUseExampleData(): Boolean {
        return preferences.getBoolean(USE_EXAMPLE_DATA_KEY, false)
    }

    companion object {
        const val LOCATION_PRIVACY_PREFERENCES = "location-privacy-preferences"
        const val LAST_LOCATION_KEY = "last-location"
        const val USE_EXAMPLE_DATA_KEY = "use-example-data"

        fun getLocationProcessors(
            context: Context, listener: LocationPrivacyToolkitListener?
        ): List<AbstractInternalLocationProcessor> {
            val processors =
                LocationPrivacyConfig.values().filter { p -> p != LocationPrivacyConfig.External }
                    .mapNotNull { c -> c.getLocationProcessor(context, listener) }
            return processors.sortedBy { p -> p.sortIndex }
        }
    }
}