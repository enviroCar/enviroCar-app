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

import androidx.annotation.ColorInt

/**
 * [Polygon]
 * ----------
 * [Polygon] may be used to draw a polygon on the map.
 * Utilize [Polygon.Builder] to create a new [Polygon].
 *
 * @property id      The unique identifier.
 * @property points  The list of geographical points that make up the polygon.
 * @property color   The fill color for the polygon.
 * @property opacity The opacity of the polygon.
 */
open class Polygon internal constructor(
    val id: Long,
    val points: List<Point>,
    @ColorInt val color: Int,
    val opacity: Float
) {
    class Builder(private val points: List<Point>) {
        @ColorInt
        private var color: Int = DEFAULT_COLOR
        private var opacity: Float = DEFAULT_OPACITY

        /** Sets the fill color for the polygon. */
        fun withColor(@ColorInt value: Int) = apply { color = value }

        /** Sets the fill opacity for the polygon. */
        fun withOpacity(value: Float) = apply { opacity = value }

        /** Builds the polygon. */
        fun build(): Polygon {
            assert(points.isNotEmpty()) {
                "Polygon must have at least one point."
            }
            return Polygon(
                count++,
                points,
                color,
                opacity
            )
        }
    }

    companion object {

        /** Creates a [Polygon] with default style. */
        fun default(points: List<Point>) = Builder(points).build()

        @Volatile
        private var count = 0L

        private const val DEFAULT_COLOR = 0xFF000000.toInt()
        private const val DEFAULT_OPACITY = 1.0F
    }
}
