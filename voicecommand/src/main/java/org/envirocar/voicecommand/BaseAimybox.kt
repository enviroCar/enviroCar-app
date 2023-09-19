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
package org.envirocar.voicecommand

import android.content.Context
import com.justai.aimybox.Aimybox
import com.justai.aimybox.core.Config.Companion.create
import com.justai.aimybox.speechkit.google.platform.GooglePlatformSpeechToText
import com.justai.aimybox.speechkit.google.platform.GooglePlatformTextToSpeech
import com.justai.aimybox.speechkit.kaldi.KaldiAssets
import com.justai.aimybox.speechkit.kaldi.KaldiVoiceTrigger
import com.squareup.otto.Bus
import org.envirocar.voicecommand.customskills.EnviroCarRasaCustomSkill
import org.envirocar.voicecommand.dialogapi.rasa.CustomRasaDialogApi
import org.envirocar.voicecommand.handlers.MetadataHandler
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author Dhiraj Chauhan
 */

@Singleton
class BaseAimybox @Inject constructor(
    context: Context,
    bus: Bus,
    metadataHandler: MetadataHandler
) {

    val mContext: Context
    val mBus: Bus
    val mMetadataHandler: MetadataHandler

    init {
        mContext = context
        mBus = bus
        mMetadataHandler = metadataHandler
    }

    lateinit var aimybox: Aimybox
    lateinit var activityContext: Context


    fun initializeAimybox() {
        if (!::aimybox.isInitialized) {
            aimybox = createAimybox(mContext, mBus, mMetadataHandler)
        }
    }

    private fun createAimybox(
        context: Context,
        mBus: Bus,
        metadataHandler: MetadataHandler
    ): Aimybox {
        // Accessing model from assets folder
        val assets = KaldiAssets.fromApkAssets(context, "model/en")

        // initializing pocketsphinx provider
        val voiceTrigger = KaldiVoiceTrigger(assets, listOf("envirocar listen"))

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.ENGLISH, false)
        val speechToText = GooglePlatformSpeechToText(context, Locale.ENGLISH, false, 10000L)

        val dialogApi = CustomRasaDialogApi(
            sender, WEBHOOK_URL, linkedSetOf(
                EnviroCarRasaCustomSkill(metadataHandler, mBus)
            )
        )

        return Aimybox(create(speechToText, textToSpeech, dialogApi) {
            this.voiceTrigger = voiceTrigger
        }, context)
    }


    companion object {
        private const val ARGUMENTS_KEY = "arguments"
        private val sender = UUID.randomUUID().toString()
        private const val WEBHOOK_URL =
            "https://rasa-server-cdhiraj40.cloud.okteto.net/webhooks/envirocar/webhook"
    }
}