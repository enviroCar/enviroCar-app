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

package org.envirocar.map.location.annotation

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
