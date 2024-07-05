package org.envirocar.map.model

/**
 * [Animation]
 * -----------
 * [Animation] may be used specify a new animation.
 * Utilize the [Animation.Builder] to create a new instance.
 *
 * @property duration The duration of the animation (in milliseconds).
 */
class Animation private constructor(
    val duration: Long
) {
    class Builder {
        private var duration: Long = DEFAULT_DURATION

        /** Sets duration of the animation (in milliseconds). */
        fun withDuration(value: Long) = apply { duration = value }

        /** Builds the animation. */
        fun build(): Animation {
            return Animation(
                duration
            )
        }

        companion object {

            /** Creates an [Animation] with default style. */
            fun default() = Builder().build()

            private const val DEFAULT_DURATION = 1000L
        }
    }
}
