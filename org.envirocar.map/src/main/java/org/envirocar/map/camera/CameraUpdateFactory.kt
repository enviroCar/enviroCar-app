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
    fun newCameraUpdateBasedOnBounds(points: List<Point>, padding: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBasedOnBounds(points, padding)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the bearing is set to the specified value.
     * Minimum bearing value is 0 and maximum bearing value is 360.
     *
     * @param bearing The bearing of the camera.
     */
    fun newCameraUpdateBearing(bearing: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateBearing(bearing)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the tilt is set to the specified value.
     * Minimum tilt value is 0 and maximum tilt value is 60.
     *
     * @param tilt The tilt of the camera.
     */
    fun newCameraUpdateTilt(tilt: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateTilt(tilt)
    }

    /**
     * Creates a [CameraUpdate] to transform the camera so that the zoom is set to the specified value.
     * Minimum zoom value is 0 and maximum zoom value is 22.
     *
     * @param zoom The zoom level of the camera.
     */
    fun newCameraUpdateZoom(zoom: Float): CameraUpdate {
        return CameraUpdate.Companion.CameraUpdateZoom(zoom)
    }

}
