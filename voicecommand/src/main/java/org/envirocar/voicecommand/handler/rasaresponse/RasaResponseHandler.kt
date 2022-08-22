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
package org.envirocar.voicecommand.handler.rasaresponse

import com.google.gson.JsonArray
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply
import org.envirocar.voicecommand.dialogapi.rasa.EnviroCarRasaResponse
import org.envirocar.voicecommand.model.CustomRasaResponse
import org.envirocar.voicecommand.utils.JsonExtensions

/**
 * @author Dhiraj Chauhan
 */
class RasaResponseHandler : AbstractResponseHandler() {

    /**
     * Handles the httpResponse as JsonArray from the Rasa server
     * and decodes as `EnviroCarRasaResponse`
     */
    override fun decodeRasaResponse(httpResponse: JsonArray): EnviroCarRasaResponse {
        val responseList = JsonExtensions.generateEnviroCarRasaResponse(httpResponse)
        val response = responseList[0]

        if (response.custom != null) {
            response.custom =
                parseCustomResponse(response)
        }
        return EnviroCarRasaResponse(
            recipient_id = response.recipient_id,
            query = response.query,
            text = response.text,
            action = response.action,
            intent = response.intent,
            question = response.question,
            replies = parseReplies(response),
            custom = response.custom,
            httpResponse = response.httpResponse
        )
    }

    /**
     * Parses the response and returns list of replies to speak.
     */
    private fun parseReplies(response: EnviroCarRasaResponse): List<Reply> {
        val replies = ArrayList<Reply>()

        if (response.text != null) {
            replies.add(TextReply(response.text))
        }
        if (response.custom?.reply != null) {
            replies.add(TextReply(response.custom?.reply))
        }
        return replies
    }

    /**
     * Parses response and returns as `CustomRasaResponse`
     */
    private fun parseCustomResponse(response: EnviroCarRasaResponse): CustomRasaResponse {
        val customResponse = response.custom!!

        // set query and other data to response
        response.query = customResponse.query


        if (customResponse.action != null) {
            response.action = customResponse.action.custom_event
        }

        return customResponse
    }

}