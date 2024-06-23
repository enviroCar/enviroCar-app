package org.envirocar.map.location

import org.envirocar.map.R
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point

/**
 *  [LocationPointMarker]
 *  -------------------------
 *  The [Marker] used to display the current location.
 */
internal class LocationPointMarker(point: Point) :
    Marker(ID, point, TITLE, DRAWABLE, SCALE, ROTATION) {

    companion object {
        private const val ID = -0xCL
        private val TITLE = null
        private val DRAWABLE = R.drawable.location_point
        private const val SCALE = 0.18F
        private const val ROTATION = 0.0F
    }
}
