package org.envirocar.map.model

import androidx.annotation.DrawableRes

/**
 * [Marker]
 * ---------
 * [Marker] may be used to indicate a specific location on the map.
 * Utilize the [Marker.Builder] to create a new instance.
 *
 * @property id       The unique identifier.
 * @property point    The geographical point.
 * @property title    The title of the marker.
 * @property drawable The drawable of the marker.
 */
class Marker private constructor(
    val id: Long,
    val point: Point,
    val title: String?,
    @DrawableRes val drawable: Int?
) {
    class Builder(private val point: Point) {
        private var title: String? = null
        @DrawableRes
        private var drawable: Int? = null

        /** Sets the title of the marker. */
        fun withTitle(value: String) = apply { title = value }

        /** Sets the drawable of the marker. */
        fun withDrawable(@DrawableRes value: Int) = apply { drawable = value }

        /** Builds the marker. */
        fun build(): Marker {
            return Marker(
                count++,
                point,
                title,
                drawable
            )
        }
    }

    companion object {

        /** Creates a [Marker] with default style. */
        fun default(point: Point) = Builder(point).build()

        @Volatile
        private var count = 0L
    }
}
