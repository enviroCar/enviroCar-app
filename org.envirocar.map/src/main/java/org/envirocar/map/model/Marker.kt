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
