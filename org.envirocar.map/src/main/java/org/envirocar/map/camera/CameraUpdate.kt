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
 * [CameraUpdate]
 * --------------
 * [CameraUpdate] is a marker interface for all camera updates.
 *
 */
sealed interface CameraUpdate {
    companion object {

        /**
         * [CameraUpdateBasedOnPoint]
         * --------------------------
         * Camera update to transform the camera so that the point is centered on screen.
         *
         * @param point The geographical point.
         */
        internal data class CameraUpdateBasedOnPoint(
            val point: Point
        ) : CameraUpdate

        /**
         * [CameraUpdateBasedOnBounds]
         * ---------------------------
         * Camera update to transform the camera so that bounds specified by the points are centered on
         * screen at the greatest possible zoom level. The padding may be used to specify additional
         * padding on each side of the bounds.
         *
         * @param points The geographical points specifying the bounds.
         * @param padding The padding in pixels.
         */
        internal data class CameraUpdateBasedOnBounds(
            val points: List<Point>,
            val padding: Float
        ) : CameraUpdate

        /**
         * [CameraUpdateBasedOnPointAndBearing]
         * --------------------------
         * Camera update to transform the camera so that the point is centered on screen & the
         * bearing is set to the specified value.
         *
         * @param point The geographical point.
         * @param bearing The bearing of the camera.
         */
        internal data class CameraUpdateBasedOnPointAndBearing(
            val point: Point,
            val bearing: Float
        ) : CameraUpdate

        /**
         * [CameraUpdateBearing]
         * ---------------------
         * Camera update to transform the camera so that the bearing is set to the specified value.
         */
        internal data class CameraUpdateBearing(
            val bearing: Float
        ) : CameraUpdate

        /**
         * [CameraUpdateTilt]
         * ------------------
         * Camera update to transform the camera so that the tilt is set to the specified value.
         */
        internal data class CameraUpdateTilt(
            val tilt: Float
        ) : CameraUpdate

        /**
         * [CameraUpdateZoom]
         * ------------------
         * Camera update to transform the camera so that the zoom is set to the specified value.
         */
        internal data class CameraUpdateZoom(
            val zoom: Float
        ) : CameraUpdate

    }
}
