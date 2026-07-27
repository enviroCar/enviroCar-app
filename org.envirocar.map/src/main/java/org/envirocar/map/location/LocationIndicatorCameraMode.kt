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
    data class Follow(val animation: Animation = Animation.Builder().withDuration(200L).build()) : LocationIndicatorCameraMode()
}
