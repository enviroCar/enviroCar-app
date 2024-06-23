package org.envirocar.map.location

import org.envirocar.map.model.Animation

/**
 * [LocationIndicatorCameraMode]
 * -----------------------------
 * [LocationIndicatorCameraMode] allows to specify the camera mode for the [LocationIndicator].
 */
sealed class LocationIndicatorCameraMode {
    /**
     * Camera does not follow the current location.
     */
    data object None : LocationIndicatorCameraMode()

    /**
     * Camera updates to the current location.
     */
    data class Follow(val animation: Animation) : LocationIndicatorCameraMode()
}
