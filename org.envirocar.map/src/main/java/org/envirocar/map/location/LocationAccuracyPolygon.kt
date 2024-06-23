package org.envirocar.map.location

import androidx.annotation.ColorInt
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polygon
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 *  [LocationAccuracyPolygon]
 *  -------------------------
 *  The [Polygon] used to display the current location accuracy in form of a circle.
 */
internal class LocationAccuracyPolygon(point: Point, radius: Float) :
    Polygon(ID, calculatePolygonPoints(point, radius), COLOR) {

    companion object {
        private fun calculatePolygonPoints(point: Point, radius: Float): List<Point> {
            val points = (radius * 32.0).toInt()
            val srcLatitude = point.latitude.toRadians()
            val srcLongitude = point.longitude.toRadians()
            val distance = radius / EARTH_RADIUS
            return MutableList(points) {
                val bearing = it * 2.0 * PI / points
                val dstLatitude = asin(
                    sin(srcLatitude) * cos(distance) +
                       cos(srcLatitude) * sin(distance) * cos(bearing)
                )
                val dstLongitude = srcLongitude + atan2(
                    sin(bearing) * sin(distance) * cos(srcLatitude),
                    cos(distance) - sin(srcLatitude) * sin(dstLatitude)
                )
                val x = dstLatitude.toDegrees()
                val y = dstLongitude.toDegrees()
                Point(x, y)
            }.apply { firstOrNull()?.let { add(it) } }
        }

        private fun Double.toDegrees() = (this * 180.0 / PI)
        private fun Double.toRadians() = (this * PI / 180.0)

        private const val EARTH_RADIUS = 6371008.8

        private const val ID = -0xAL

        @ColorInt
        private const val COLOR = 0x144384F4
    }
}
