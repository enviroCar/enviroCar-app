/**
 * Copyright (C) 2013 - 2022 the enviroCar community
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
package org.envirocar.voicecommand.handler

import org.envirocar.voicecommand.model.ExtraMetadata
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Dhiraj Chauhan
 */
@Singleton
class MetadataHandler @Inject constructor() {
    var metadata: ExtraMetadata? = null

    fun setDashboardFragmentVisibility() {
        metadata?.isDashboardFragment = false
    }

    fun onDashboardFragmentTrue() {
        metadata?.isDashboardFragment = true
    }

    fun onRecordingScreenFalse() {
        metadata?.recordingMetadata?.is_recording_screen = false
    }

    fun onRecordingScreenTrue() {
        metadata?.recordingMetadata?.is_recording_screen = true
    }

    fun onCarSelectionFragmentFalse() {
        metadata?.car_selection_metadata?.is_car_selection_fragment = false
    }

    fun onCarSelectionFragmentTrue() {
        metadata?.car_selection_metadata?.is_car_selection_fragment = true
    }
}