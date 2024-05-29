package org.envirocar.map.model

import android.graphics.Color

/**
 * [Polyline]
 * ----------
 * [Polyline] represents a line on the map.
 * Utilize [Polyline.Builder] to create a new [Polyline].
 */
sealed interface Polyline {
    class Builder(private val points: List<Point>) {
        private var width: Float = 2.0F
        private var color: Color? = null
        private var colors: List<Color>? = null

        /** Sets the width of the polyline. */
        fun withWidth(value: Float) = apply { width = value }

        /** Sets the color of the polyline, for displaying a single color polyline. */
        fun withColor(value: Color) = apply { color = value }

        /** Sets the list of colors of the polyline, for displaying a gradient polyline. */
        fun withColors(value: List<Color>) = apply { colors = value }

        /** Builds the polyline. */
        fun build(): Polyline {
            assert(points.isNotEmpty()) {
                "Polyline must have at least one point."
            }
            assert(color != null && colors == null || color == null && colors != null) {
                "Polyline must have either a single color or a gradient."
            }
            assert(colors?.isNotEmpty() ?: true) {
                "Polyline must have one less color than points for a gradient."
            }
            assert(colors?.size == points.size) {
                "Polyline must have same number of colors as points for a gradient."
            }
            return PolylineImpl(
                count++,
                points,
                width,
                color,
                colors
            )
        }
    }

    companion object {
        @Volatile
        private var count = 0

        /**
         * [PolylineImpl]
         * --------------
         * [PolylineImpl] is the implementation of the [Polyline] interface.
         *
         * @property id     The unique identifier.
         * @property points The list of geographical points that make up the polyline.
         * @property color  The color for displaying a single color polyline.
         * @property colors The list of colors for displaying a gradient polyline.
         */
        internal data class PolylineImpl(
            val id: Int,
            val points: List<Point>,
            val width: Float,
            val color: Color?,
            val colors: List<Color>?
        ) : Polyline

    }
}
