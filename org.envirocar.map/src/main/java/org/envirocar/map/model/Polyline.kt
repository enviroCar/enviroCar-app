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
 * [Polyline]
 * ----------
 * [Polyline] may be used to draw a polyline on the map.
 * Utilize [Polyline.Builder] to create a new [Polyline].
 *
 * @property id          The unique identifier.
 * @property points      The list of geographical points that make up the polyline.
 * @property color       The color for displaying a single color polyline.
 * @property borderWidth The border width of the polyline.
 * @property borderColor The border color of the polyline.
 * @property colors      The list of colors for displaying a gradient polyline.
 */
open class Polyline internal constructor(
    val id: Long,
    val points: List<Point>,
    val width: Float,
    @ColorInt val color: Int,
    val borderWidth: Float,
    @ColorInt val borderColor: Int,
    val colors: List<Int>?
) {
    class Builder(private val points: List<Point>) {
        private var width: Float = DEFAULT_WIDTH
        @ColorInt
        private var color: Int = DEFAULT_COLOR
        private var borderWidth: Float = DEFAULT_BORDER_WIDTH
        @ColorInt
        private var borderColor: Int = DEFAULT_BORDER_COLOR
        private var colors: List<Int>? = null

        /** Sets the width of the polyline. */
        fun withWidth(value: Float) = apply { width = value }

        /** Sets the color of the polyline, for displaying a single color polyline. */
        fun withColor(@ColorInt value: Int) = apply { color = value }

        /** Sets the width of the border of the polyline. */
        fun withBorderWidth(value: Float) = apply { borderWidth = value }

        /** Sets the color of the border of the polyline. */
        fun withBorderColor(@ColorInt value: Int) = apply { borderColor = value }

        /** Sets the list of colors of the polyline, for displaying a gradient polyline. */
        fun withColors(value: List<Int>) = apply { colors = value }

        /** Builds the polyline. */
        fun build(): Polyline {
            assert(points.isNotEmpty()) {
                "Polyline must have at least one point."
            }
            assert(colors == null || colors?.size == points.size) {
                "Polyline must have same number of colors as points for a gradient."
            }
            return Polyline(
                count++,
                points,
                width,
                color,
                borderWidth,
                borderColor,
                colors
            )
        }
    }

    companion object {

        /** Creates a [Polyline] with default style. */
        fun default(points: List<Point>) = Builder(points).build()

        @Volatile
        private var count = 0L

        private const val DEFAULT_WIDTH = 2.0F
        private const val DEFAULT_COLOR = 0xFF000000.toInt()
        private const val DEFAULT_BORDER_WIDTH = 0.0F
        private const val DEFAULT_BORDER_COLOR = 0xFFFFFFFF.toInt()
    }
}
