package org.envirocar.map.camera

import kotlinx.coroutines.flow.MutableStateFlow
import org.envirocar.map.MapView.Companion.CAMERA_BEARING_DEFAULT
import org.envirocar.map.MapView.Companion.CAMERA_POINT_LATITUDE_DEFAULT
import org.envirocar.map.MapView.Companion.CAMERA_POINT_LONGITUDE_DEFAULT
import org.envirocar.map.MapView.Companion.CAMERA_TILT_DEFAULT
import org.envirocar.map.MapView.Companion.CAMERA_ZOOM_DEFAULT
import org.envirocar.map.model.Point

internal class MutableCameraState: CameraState {
    override val position = MutableStateFlow(Point(CAMERA_POINT_LATITUDE_DEFAULT, CAMERA_POINT_LONGITUDE_DEFAULT))
    override val bearing = MutableStateFlow(CAMERA_BEARING_DEFAULT)
    override val tilt = MutableStateFlow(CAMERA_TILT_DEFAULT)
    override val zoom = MutableStateFlow(CAMERA_ZOOM_DEFAULT)
}
