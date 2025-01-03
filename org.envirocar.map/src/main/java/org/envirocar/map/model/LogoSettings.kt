package org.envirocar.map.model

import android.view.Gravity

/**
 * [LogoSettings]
 * ---------------------
 * [LogoSettings] encapsulates various options related to logo present on the map.
 * Utilize the [LogoSettings.Builder] to create a new instance.
 *
 * @property enabled Whether the logo is enabled or not.
 * @property gravity The gravity for the logo view.
 * @property margin  The margin for the logo view.
 */
class LogoSettings internal constructor(
    val enabled: Boolean,
    val gravity: Int,
    val margin: FloatArray
) {
    class Builder {
        private var enabled: Boolean = true
        private var gravity: Int = Gravity.BOTTOM or Gravity.START
        private var margin: FloatArray = floatArrayOf(12.0F, 12.0F, 12.0F, 12.0F)

        /** Sets whether the logo is enabled or not. */
        fun withEnabled(value: Boolean) = apply { enabled = value }

        /** Sets the gravity for the logo view. */
        fun withGravity(value: Int) = apply { gravity = value }

        /** Sets the margin for the logo view. */
        fun withMargin(value: FloatArray) = apply { margin = value }

        /** Builds the logo settings. */
        fun build(): LogoSettings {
            return LogoSettings(
                enabled,
                gravity,
                margin
            )
        }
    }

    companion object {

        /** Creates a [LogoSettings] with default style. */
        fun default() = Builder().build()
    }
}
