package org.envirocar.map.model

import android.view.Gravity

/**
 * [AttributionSettings]
 * ---------------------
 * [AttributionSettings] encapsulates various options related to attribution present on the map.
 * Utilize the [AttributionSettings.Builder] to create a new instance.
 *
 * @property enabled Whether the attribution is enabled or not.
 * @property gravity The gravity for the attribution view.
 * @property margin  The margin for the attribution view.
 */
class AttributionSettings internal constructor(
    val enabled: Boolean,
    val gravity: Int,
    val margin: FloatArray
) {
    class Builder {
        private var enabled: Boolean = true
        private var gravity: Int = Gravity.BOTTOM or Gravity.START
        private var margin: FloatArray = floatArrayOf(320.0F, 12.0F, 12.0F, 12.0F)

        /** Sets whether the attribution is enabled or not. */
        fun withEnabled(value: Boolean) = apply { enabled = value }

        /** Sets the gravity for the attribution view. */
        fun withGravity(value: Int) = apply { gravity = value }

        /** Sets the margin for the attribution view. */
        fun withMargin(value: FloatArray) = apply { margin = value }

        /** Builds the attribution settings. */
        fun build(): AttributionSettings {
            return AttributionSettings(
                enabled,
                gravity,
                margin
            )
        }
    }

    companion object {

        /** Creates a [AttributionSettings] with default style. */
        fun default() = Builder().build()
    }
}
