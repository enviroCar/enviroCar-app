package org.envirocar.map.model

import androidx.annotation.DrawableRes

/**
 * [Marker]
 * ---------
 * [Marker] may be used to indicate a specific location on the map.
 * Utilize the [Marker.Builder] to create a new instance.
 */
sealed interface Marker {
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
            return MarkerImpl(
                count++,
                point,
                title,
                drawable
            )
        }
    }

    companion object {
        @Volatile
        private var count = 0

        /**
         * [MarkerImpl]
         * ------------
         * [MarkerImpl] is the implementation of the [Marker] interface.
         *
         * @property id       The unique identifier.
         * @property point    The geographical point.
         * @property title    The title of the marker.
         * @property drawable The drawable of the marker.
         */
        internal data class MarkerImpl(
            val id: Int,
            val point: Point,
            val title: String? = null,
            @DrawableRes val drawable: Int?,
        ) : Marker

    }
}
