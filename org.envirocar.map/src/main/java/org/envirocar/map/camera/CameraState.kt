package org.envirocar.map.camera

import kotlinx.coroutines.flow.StateFlow
import org.envirocar.map.model.Point

/**
 * [CameraState]
 * -------------
 * [CameraState] provides access to various camera attributes as [StateFlow].
 */
interface CameraState {
    /** Position. */
    val position: StateFlow<Point>

    /** Bearing. */
    val bearing: StateFlow<Float>

    /** Tilt. */
    val tilt: StateFlow<Float>

    /** Zoom. */
    val zoom: StateFlow<Float>
}
