package org.envirocar.voicecommand.model

import org.envirocar.voicecommand.enums.ModelState

data class AvailableVoiceModel (
    val status: ModelState,
    val response: List<VoiceModel>?
)