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
