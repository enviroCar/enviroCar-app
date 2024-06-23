package org.envirocar.map.location

import org.envirocar.map.R
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point

/**
 *  [LocationBearingMarker]
 *  -------------------------
 *  The [Marker] used to display the current location's bearing.
 */
internal class LocationBearingMarker(point: Point, bearing: Float) :
    Marker(ID, point, TITLE, DRAWABLE, SCALE, bearing) {

    companion object {
        private const val ID = -0xBL
        private val TITLE = null
        private val DRAWABLE = R.drawable.location_bearing
        private const val SCALE = 0.2F
    }
}
