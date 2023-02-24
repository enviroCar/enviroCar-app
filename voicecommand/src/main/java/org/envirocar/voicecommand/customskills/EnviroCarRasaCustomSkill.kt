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
package org.envirocar.voicecommand.customskills

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import com.justai.aimybox.Aimybox
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.model.reply.AudioReply
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.model.reply.asAudioSpeech
import com.justai.aimybox.model.reply.asTextSpeech
import com.squareup.otto.Bus
import org.envirocar.voicecommand.dialogapi.rasa.EnviroCarRasaRequest
import org.envirocar.voicecommand.dialogapi.rasa.EnviroCarRasaResponse
import org.envirocar.voicecommand.handler.MetadataHandler
import org.envirocar.voicecommand.handler.rasaresponse.RasaResponseHandler
import org.envirocar.voicecommand.intention.EnviroCarIntention
import java.util.concurrent.CancellationException

/**
 * @author Dhiraj Chauhan
 *
 * This is a custom skill that enables the voice assistant to perform any actions right on the
 * device from where the user speaks their voice commands.
 * docs: https://help.aimybox.com/en/article/android-custom-skills-1a1j0x0/
 */
class EnviroCarRasaCustomSkill(private val metadataHandler: MetadataHandler, private val bus: Bus) :
    AbstractEnviroCarCustomSkill(metadataHandler, bus) {

    override fun canHandle(response: EnviroCarRasaResponse): Boolean = true

    /**
     * This method will be called by Aimybox service right after the user's speech was recognised.
     */
    override suspend fun onRequest(
        request: EnviroCarRasaRequest,
        aimybox: Aimybox
    ): EnviroCarRasaRequest {

        request.data = metadataHandler.metadata
        return request
    }

    /**
     * The main method of the custom skill that should perform the actual action for the particular
     * dialog API's Response.
     */
    @SuppressLint("MissingPermission")
    override suspend fun onResponse(
        response: EnviroCarRasaResponse,
        aimybox: Aimybox,
        defaultHandler: suspend (Response) -> Unit
    ) {
        // decoding the response from rasa
        val rasaResponseHandler = RasaResponseHandler()

        if (response.httpResponse != null && response.httpResponse.size() != 0) {
            val rasaResponse = rasaResponseHandler.decodeRasaResponse(response.httpResponse)

            // if event based actions then post events else default handle
            if (rasaResponse.action != null && rasaResponse.actionType != null && rasaResponse.custom?.action?.next_action != null) {
                EnviroCarIntention.postEvent(
                    bus,
                    aimybox,
                    rasaResponse.custom?.data,
                    rasaResponse.action!!,
                    rasaResponse.actionType!!,
                    rasaResponse.custom?.action?.next_action!!
                )
                speakResponse(rasaResponse, aimybox, rasaResponse.custom?.action?.next_action!!)
            } else if (rasaResponse.custom?.action?.next_action != null) {
                // if response is not event based but has a next action
                speakResponse(rasaResponse, aimybox, rasaResponse.custom?.action?.next_action!!)
            } else {
                speakResponse(rasaResponse, aimybox, Aimybox.NextAction.STANDBY)
            }
        } else {
            aimybox.speak(
                TextSpeech("Something went wrong. Please try again."),
                Aimybox.NextAction.STANDBY
            )
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun speakResponse(
        response: EnviroCarRasaResponse,
        aimybox: Aimybox,
        nextAction: Aimybox.NextAction
    ) {
        try {
            val speeches = response.replies
                .filter { it is TextReply || it is AudioReply }
                .map {
                    when (it) {
                        is TextReply -> it.asTextSpeech()
                        is AudioReply -> it.asAudioSpeech()
                        else -> throw IllegalArgumentException("Reply type is not supported by default handler")
                    }
                }

            speeches.takeIf { it.isNotEmpty() }?.let { it ->

                try {
                    val filteredSpeeches = it.filter { speech ->
                        !(speech is TextSpeech && speech.text.isEmpty())
                    }
                    if (filteredSpeeches.isNotEmpty()) {
                        aimybox.speak(filteredSpeeches, nextAction)?.join()
                    } else {
                        aimybox.standby()
                    }
                } catch (e: CancellationException) {
                    Log.w("Speech cancelled", e)
                }
            } ?: aimybox.standby()

        } catch (e: Throwable) {
            Log.e("parsing replies from $response", e.message.toString())
        }
    }
}



