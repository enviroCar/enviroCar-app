package org.envirocar.map.location.annotation

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import org.envirocar.map.R
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point

/**
 *  [LocationPointMarker]
 *  -------------------------
 *  The [Marker] used to display the current location.
 */
internal class LocationPointMarker(
    point: Point,
    context: Context
) : Marker(
    ID,
    point,
    TITLE,
    DRAWABLE,
    bitmap ?: synchronized(lock) {
        bitmap ?: AppCompatResources.getDrawable(context, R.drawable.location_point)!!.toBitmap()
            .also { bitmap = it }
    },
    SCALE,
    ROTATION
) {

    companion object {
        private val lock = Any()

        @Volatile
        private var bitmap: Bitmap? = null

        private const val ID = -0xCL
        private val TITLE = null
        private val DRAWABLE = null
        private const val SCALE = 0.18F
        private const val ROTATION = 0.0F
    }
}
