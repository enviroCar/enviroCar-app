package de.fh.muenster.locationprivacytoolkit.processors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.*
import androidx.fragment.app.Fragment
import de.fh.muenster.locationprivacytoolkit.LocationPrivacyToolkitListener
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface

/**
 * Abstract class which processes a location
 * It's meant to be implemented by a subclass
 * which defines its logic in the `process` function
 */
abstract class AbstractInternalLocationProcessor(
    context: Context, var listener: LocationPrivacyToolkitListener? = null
) : BroadcastReceiver() {

    // configuration the subclass is processing. Must be implemented by subclass
    abstract val config: LocationPrivacyConfig

    // Sort key which determines the order to execute the processors
    open val sortIndex: Int
        get() = config.sortIndex

    // labels to show in configuration ui
    abstract val titleId: Int
    abstract val subtitleId: Int
    abstract val descriptionId: Int

    // user interface that is shown in configuration ui
    abstract val userInterface: LocationProcessorUserInterface
    open val fragment: Fragment? = null

    // possible config values
    open val defaultValue: Int = 0
    open val values: Array<Int> = emptyArray()
    open val valueRange: IntRange
        get() = IntRange(0, values.size - 1)

    open fun formatLabel(value: Int): String? = null

    // actual config from the LocationPrivacyConfig
    internal val locationPrivacyConfig: LocationPrivacyConfigManager =
        LocationPrivacyConfigManager(context)

    /**
     * Function to get the corresponding config value
     *
     * @return Value of the LocationPrivacyConfig
     */
    private val configValue: Int?
        get() = locationPrivacyConfig.getPrivacyConfig(config)

    /**
     * A guard that checks the input parameters. If there is no location or no
     * configuration, it either returns null or the given location
     * Otherwise it returns the result of the processor
     *
     * @param location A location object or null
     * @return A manipulated location, the original location or null
     */
    fun process(location: Location?): Location? {
        // return null if no location is provided
        if (location == null) {
            return null
        }
        // get config or return location if config is null
        val config = configValue ?: -1

        return this.manipulateLocation(location, config)
    }

    /**
     * The function that manipulates a location
     *
     * @param location A location object that will be manipulated
     * @param config Value of the LocationPrivacyConfig
     * @return A manipulated location
     */
    abstract fun manipulateLocation(location: Location, config: Int): Location?

    // helper methods to convert between indices and values
    fun indexToValue(indexValue: Float): Int? {
        val configValues = this.values
        if (configValues.isNotEmpty()) {
            val index = indexValue.toInt()
            return configValues.getOrNull(index)
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

    // BroadcastReceiver

    override fun onReceive(context: Context, intent: Intent?) {
        // implement in sub-classes if needed
    }
}