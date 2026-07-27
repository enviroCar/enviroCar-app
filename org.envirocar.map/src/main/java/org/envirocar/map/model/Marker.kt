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

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import org.envirocar.map.R

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
 * @property bitmap   The bitmap of the marker.
 * @property scale    The scale of the marker.
 * @property rotation The rotation of the marker.
 */
open class Marker internal constructor(
    val id: Long,
    val point: Point,
    val title: String?,
    @DrawableRes val drawable: Int?,
    val bitmap: Bitmap?,
    val scale: Float,
    val rotation: Float
) {
    class Builder(private val point: Point) {
        private var title: String? = null
        @DrawableRes
        private var drawable: Int? = DEFAULT_DRAWABLE
        private var bitmap: Bitmap? = null
        private var scale: Float = 1.0F
        private var rotation: Float = 0.0F

        /** Sets the title of the marker. */
        fun withTitle(value: String) = apply { title = value }

        /** Sets the drawable of the marker. */
        fun withDrawable(@DrawableRes value: Int) = apply { drawable = value }

        /** Sets the bitmap of the marker. */
        fun withBitmap(value: Bitmap) = apply { bitmap = value }

        /** Sets the scale of the marker. */
        fun withScale(value: Float) = apply { scale = value }

        /** Sets the rotation of the marker. */
        fun withRotation(value: Float) = apply { rotation = value }

        /** Builds the marker. */
        fun build(): Marker {
            assert(drawable == null || bitmap == null) {
                "Marker must have either drawable or bitmap."
            }
            return Marker(
                count++,
                point,
                title,
                drawable,
                bitmap,
                scale,
                rotation
            )
        }
    }

    companion object {

        /** Creates a [Marker] with default style. */
        fun default(point: Point) = Builder(point).build()

        @Volatile
        private var count = 0L

        private val DEFAULT_DRAWABLE = R.drawable.marker_icon_default
    }
}
