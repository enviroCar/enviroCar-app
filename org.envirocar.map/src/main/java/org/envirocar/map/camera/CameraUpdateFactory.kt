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

package org.envirocar.map.camera

import org.envirocar.map.model.Point

/**
 * [CameraUpdateFactory]
 * ----------------------
 * [CameraUpdateFactory] is a factory class for creating [CameraUpdate]s.
 */
object CameraUpdateFactory {

    /**
     * Creates a [CameraUpdate] to transform the camera so that the point is centered on screen.
     *
     * @param point The geographical point.
     */
    @JvmStatic
    fun newCameraUpdateBasedOnPoint(point: Point): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBasedOnPoint(point)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that bounds specified by the points
     * are centered on screen at the greatest possible zoom level. The padding may be used to
     * specify additional padding on each side of the bounds.
     *
     * @param points The geographical points specifying the bounds.
     * @param padding The padding in pixels.
     */
    @JvmStatic
    fun newCameraUpdateBasedOnBounds(points: List<Point>, padding: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBasedOnBounds(points, padding)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the point is centered on screen & the
     * bearing is set to the specified value.
     *
     * @param point The geographical point.
     * @param bearing The bearing of the camera.
     */
    @JvmStatic
    fun newCameraUpdateBasedOnPointAndBearing(point: Point, bearing: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBasedOnPointAndBearing(point, bearing)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the bearing is set to the specified value.
     * Minimum bearing value is 0 and maximum bearing value is 360.
     *
     * @param bearing The bearing of the camera.
     */
    @JvmStatic
    fun newCameraUpdateBearing(bearing: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBearing(bearing)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the tilt is set to the specified value.
     * Minimum tilt value is 0 and maximum tilt value is 60.
     *
     * @param tilt The tilt of the camera.
     */
    @JvmStatic
    fun newCameraUpdateTilt(tilt: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateTilt(tilt)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the zoom is set to the specified value.
     * Minimum zoom value is 0 and maximum zoom value is 22.
     *
     * @param zoom The zoom level of the camera.
     */
    @JvmStatic
    fun newCameraUpdateZoom(zoom: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateZoom(zoom)
    }

}
