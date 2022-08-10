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
import com.justai.aimybox.dialogapi.rasa.RasaDialogApi
import com.justai.aimybox.speechkit.google.platform.GooglePlatformSpeechToText
import com.justai.aimybox.speechkit.google.platform.GooglePlatformTextToSpeech
import com.justai.aimybox.speechkit.kaldi.KaldiAssets.Companion.fromApkAssets
import com.justai.aimybox.speechkit.kaldi.KaldiVoiceTrigger
import java.util.*

class BaseAimybox {

    fun createAimybox(context: Context): Aimybox {

        // Accessing model from assets folder
        val assets = fromApkAssets(context, "model/en")

        // initializing trigger words
        val voiceTrigger = KaldiVoiceTrigger(assets, listOf("listen", "hey car"))
        val sender = UUID.randomUUID().toString()
        val webhookUrl = "https://rasa-server-cdhiraj40.cloud.okteto.net/webhooks/rest/webhook"

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault(), false)
        val speechToText = GooglePlatformSpeechToText(context, Locale.getDefault(), false, 10000L)

        val dialogApi = RasaDialogApi(sender, webhookUrl, linkedSetOf())

        return Aimybox(create(speechToText, textToSpeech, dialogApi) {
//            this.voiceTrigger = voiceTrigger
        }, context)
    }

    fun findAimyboxProvider(activity: Activity): AimyboxProvider? {
        val application = activity.application
        return when {
            activity is AimyboxProvider -> activity
            application is AimyboxProvider -> application
            else -> null
        }
    }


    fun setInitialPhrase(
        context: Context,
        arguments: Bundle?,
        viewModel: AimyboxAssistantViewModel
    ) {
        val initialPhrase = arguments?.getString(ARGUMENTS_KEY)
            ?: context.getString(R.string.initial_phrase)

        viewModel.setInitialPhrase(initialPhrase)
    }

    companion object {
        const val ARGUMENTS_KEY = "arguments"
    }

}