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
package org.envirocar.voicecommand.intention

import com.google.gson.JsonObject
import com.justai.aimybox.Aimybox
import com.squareup.otto.Bus
import org.envirocar.voicecommand.enums.CarSelection
import org.envirocar.voicecommand.enums.NavigationScreens
import org.envirocar.voicecommand.enums.Recording
import org.envirocar.voicecommand.enums.RecordingRequirements
import org.envirocar.voicecommand.events.VoiceCommandEventType
import org.envirocar.voicecommand.events.carselection.CarSelectionEvent
import org.envirocar.voicecommand.events.navigation.NavigationEvent
import org.envirocar.voicecommand.events.recording.RecordingRequirementEvent
import org.envirocar.voicecommand.events.recording.RecordingTrackEvent

/**
 * @author Dhiraj Chauhan
 */

class EnviroCarIntention {
    companion object {

        fun postEvent(
            bus: Bus,
            aimybox: Aimybox,
            data: JsonObject?,
            action: String,
            actionType: String,
            nextAction: Aimybox.NextAction
        ) {
            when (actionType) {
                VoiceCommandEventType.Recording.name -> {
                    postRecordingEvent(bus, aimybox, action, nextAction)
                }
                VoiceCommandEventType.RecordingRequirements.name -> {
                    postRecordingRequirementEvent(bus, aimybox, action, nextAction)
                }
                VoiceCommandEventType.CarSelection.name -> {
                    postCarSelectionEvent(bus, aimybox, action, data, nextAction)
                }
                VoiceCommandEventType.NavigationScreens.name -> {
                    postNavigationEvent(bus, aimybox, action, nextAction)
                }
            }

        }

        private fun postRecordingEvent(
            bus: Bus,
            aimybox: Aimybox,
            action: String,
            nextAction: Aimybox.NextAction
        ) {
            when (action) {
                Recording.START.name -> {
                    bus.post(
                        RecordingTrackEvent(
                            aimybox,
                            Recording.START,
                            nextAction
                        )
                    )
                }
                Recording.STOP.name -> {
                    bus.post(
                        RecordingTrackEvent(
                            aimybox,
                            Recording.STOP,
                            nextAction
                        )
                    )
                }
                Recording.DISTANCE.name -> {
                    bus.post(
                        RecordingTrackEvent(
                            aimybox,
                            Recording.DISTANCE,
                            nextAction
                        )
                    )
                }
                Recording.CHANGE_VIEW.name -> {
                    bus.post(
                        RecordingTrackEvent(
                            aimybox,
                            Recording.CHANGE_VIEW,
                            nextAction
                        )
                    )
                }
                Recording.TRAVEL_TIME.name -> {
                    bus.post(
                        RecordingTrackEvent(
                            aimybox,
                            Recording.TRAVEL_TIME,
                            nextAction
                        )
                    )
                }
            }
        }

        private fun postCarSelectionEvent(
            bus: Bus,
            aimybox: Aimybox,
            action: String,
            data: JsonObject?,
            nextAction: Aimybox.NextAction
        ) {
            val carName = data?.get("car_name")?.asString.toString()
            when (action) {
                CarSelection.SELECT.name -> {
                    bus.post(
                        CarSelectionEvent(
                            aimybox,
                            CarSelection.SELECT,
                            carName,
                            nextAction
                        )
                    )
                }
                CarSelection.DESELECT.name -> {
                    bus.post(
                        CarSelectionEvent(
                            aimybox,
                            CarSelection.DESELECT,
                            nextAction = nextAction
                        )
                    )
                }
                CarSelection.DELETE.name -> {
                    bus.post(
                        CarSelectionEvent(
                            aimybox,
                            CarSelection.DELETE,
                            nextAction = nextAction
                        )
                    )
                }
            }
        }

        private fun postNavigationEvent(
            bus: Bus,
            aimybox: Aimybox,
            action: String,
            nextAction: Aimybox.NextAction
        ) {
            when (action) {
                NavigationScreens.CAR_SELECTION.name -> {
                    bus.post(
                        NavigationEvent(
                            aimybox,
                            NavigationScreens.CAR_SELECTION,
                            nextAction
                        )
                    )
                }
                NavigationScreens.RECORDING.name -> {
                    bus.post(
                        NavigationEvent(
                            aimybox,
                            NavigationScreens.RECORDING,
                            nextAction
                        )
                    )
                }
            }
        }

        private fun postRecordingRequirementEvent(
            bus: Bus,
            aimybox: Aimybox,
            action: String,
            nextAction: Aimybox.NextAction
        ) {
            when (action) {
                RecordingRequirements.GPS.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.GPS,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.CAR.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.CAR,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.BLUETOOTH.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.BLUETOOTH,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.OBD.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.OBD,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.DASHBOARD.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.DASHBOARD,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.LOCATION_PERMS.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.LOCATION_PERMS,
                            nextAction
                        )
                    )
                }
                RecordingRequirements.BLUETOOTH_PERMS.name -> {
                    bus.post(
                        RecordingRequirementEvent(
                            aimybox,
                            RecordingRequirements.BLUETOOTH_PERMS,
                            nextAction
                        )
                    )
                }
            }
        }
    }
}