/**
 * Copyright (C) 2013 - 2026 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */

package org.envirocar.map.model

/**
 * [Animation]
 * -----------
 * [Animation] may be used specify a new animation.
 * Utilize the [Animation.Builder] to create a new instance.
 *
 * @property duration The duration of the animation (in milliseconds).
 */
open class Animation internal constructor(
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
