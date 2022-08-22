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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.example.voicecommand.R
import com.justai.aimybox.Aimybox
import com.justai.aimybox.components.AimyboxAssistantViewModel
import com.justai.aimybox.components.AimyboxProvider
import com.justai.aimybox.core.Config.Companion.create
import com.justai.aimybox.speechkit.google.platform.GooglePlatformSpeechToText
import com.justai.aimybox.speechkit.google.platform.GooglePlatformTextToSpeech
import com.justai.aimybox.speechkit.pocketsphinx.PocketsphinxAssets
import com.justai.aimybox.speechkit.pocketsphinx.PocketsphinxRecognizerProvider
import com.justai.aimybox.speechkit.pocketsphinx.PocketsphinxVoiceTrigger
import com.squareup.otto.Bus
import org.envirocar.voicecommand.customskills.EnviroCarRasaCustomSkill
import org.envirocar.voicecommand.dialogapi.CustomRasaDialogApi
import org.envirocar.voicecommand.handler.MetadataHandler
import java.util.*


/**
 * @author Dhiraj Chauhan
 */

class BaseAimybox (
    context: Context,
    bus: Bus,
    metadataHandler: MetadataHandler
) {

    var aimybox: Aimybox

    init {
        aimybox = createAimybox(context, bus, metadataHandler)
    }

    private fun createAimybox(
        context: Context,
        mBus: Bus,
        metadataHandler: MetadataHandler
    ): Aimybox {

        // Accessing model from assets folder
        val assets = PocketsphinxAssets
            .fromApkAssets(
                context,
                acousticModelFileName = "model/en",
                dictionaryFileName = "model/en/dictionary.dict"
            )

        // initializing pocketsphinx provider
        val provider = PocketsphinxRecognizerProvider(assets, keywordThreshold = 1e-40f)

        // initializing trigger words
        val voiceTrigger = PocketsphinxVoiceTrigger(
            provider,
            context.getString(R.string.keyphrase_envirocar_listen)
        )

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault(), false)
        val speechToText = GooglePlatformSpeechToText(context, Locale.getDefault(), false, 10000L)

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


        fun setInitialPhrase(
            context: Context,
            arguments: Bundle?,
            viewModel: AimyboxAssistantViewModel
        ) {
            val initialPhrase = arguments?.getString(ARGUMENTS_KEY)
                ?: context.getString(R.string.initial_phrase)

            viewModel.setInitialPhrase(initialPhrase)

        }

        fun findAimyboxProvider(activity: Activity): AimyboxProvider? {
            val application = activity.application
            return when {
                activity is AimyboxProvider -> activity
                application is AimyboxProvider -> application
                else -> null
            }
        }
    }
}
