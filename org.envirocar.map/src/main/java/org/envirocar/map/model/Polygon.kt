package org.envirocar.map.model

import androidx.annotation.ColorInt

/**
 * [Polygon]
 * ----------
 * [Polygon] may be used to draw a polygon on the map.
 * Utilize [Polygon.Builder] to create a new [Polygon].
 *
 * @property id     The unique identifier.
 * @property points The list of geographical points that make up the polygon.
 * @property color  The fill color for the polygon.
 */
open class Polygon internal constructor(
    val id: Long,
    val points: List<Point>,
    @ColorInt val color: Int
) {
    class Builder(private val points: List<Point>) {
        @ColorInt
        private var color: Int = DEFAULT_COLOR

        /** Sets the fill color for the polygon. */
        fun withColor(@ColorInt value: Int) = apply { color = value }

        /** Builds the polygon. */
        fun build(): Polygon {
            assert(points.isNotEmpty()) {
                "Polygon must have at least one point."
            }
            return Polygon(
                count++,
                points,
                color
            )
        }
    }

    companion object {

        /** Creates a [Polygon] with default style. */
        fun default(points: List<Point>) = Builder(points).build()

        @Volatile
        private var count = 0L

        private const val DEFAULT_COLOR = 0xFF000000.toInt()
    }
}
