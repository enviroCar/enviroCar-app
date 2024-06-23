package org.envirocar.map.location

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import org.envirocar.map.R
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point

/**
 *  [LocationBearingMarker]
 *  -------------------------
 *  The [Marker] used to display the current location's bearing.
 */
internal class LocationBearingMarker(
    point: Point,
    bearing: Float,
    context: Context
) : Marker(
    ID,
    point,
    TITLE,
    DRAWABLE,
    bitmap ?: synchronized(lock) {
         bitmap ?: AppCompatResources.getDrawable(context, R.drawable.location_bearing)!!.toBitmap()
             .also { bitmap = it }
    },
    SCALE,
    bearing
) {

    companion object {
        private val lock = Any()

        @Volatile
        private var bitmap: Bitmap? = null

        private const val ID = -0xBL
        private val TITLE = null
        private val DRAWABLE = null
        private const val SCALE = 0.2F
    }
}
