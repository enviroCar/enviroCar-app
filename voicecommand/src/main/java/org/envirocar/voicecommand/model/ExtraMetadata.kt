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
package org.envirocar.voicecommand.model

/**
 * @author Dhiraj Chauhan
 */

data class ExtraMetadata(
    val type: String,
    var isDashboardFragment: Boolean,
    val recordingMetadata: RecordingMetadata? = null,
    var car_selection_metadata: CarSelectionMetadata? = null,
)

data class RecordingMetadata(
    var recording_status: String,
    val recording_mode: String,
    var is_recording_screen: Boolean,
    val gps: Boolean,
    val car: Boolean,
    val bluetooth: Boolean,
    val obd_adapter: Boolean,
    val has_location_permission: Boolean,
    val has_bluetooth_permission: Boolean,
)

data class CarSelectionMetadata(
    val cars: List<String>,
    var is_car_selection_fragment: Boolean,
)