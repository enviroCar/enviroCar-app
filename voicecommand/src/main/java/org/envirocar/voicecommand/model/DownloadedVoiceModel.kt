package org.envirocar.voicecommand.model

import org.envirocar.voicecommand.enums.ModelState

data class DownloadedVoiceModel(
    val status: ModelState,
    val response: List<String>?
)
