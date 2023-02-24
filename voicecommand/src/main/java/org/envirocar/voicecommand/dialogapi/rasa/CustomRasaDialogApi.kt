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
package org.envirocar.voicecommand.dialogapi.rasa

import com.google.gson.Gson
import com.google.gson.JsonParser.parseString
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor


/**
 * Rasa.ai dialog API implementation
 *
 * @param sender a unique user's identifier
 * @param url - Rasa custom `envirocar` webhook URL
 */
class CustomRasaDialogApi(
    private val sender: String,
    private val url: String,
    override val customSkills: LinkedHashSet<CustomSkill<EnviroCarRasaRequest, EnviroCarRasaResponse>> = linkedSetOf()
) : DialogApi<EnviroCarRasaRequest, EnviroCarRasaResponse>() {

    private val httpClient: OkHttpClient
    private val gson = Gson()

    init {
        val builder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        httpClient = builder.build()
    }

    override fun createRequest(query: String) =
        EnviroCarRasaRequest(query, sender)

    override suspend fun send(request: EnviroCarRasaRequest): EnviroCarRasaResponse {
        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(request).toRequestBody())
            .build()

        val response = httpClient.newCall(req).execute()

        return EnviroCarRasaResponse(
            request.query,
            request.sender,
            httpResponse = parseString(response.body?.string()).asJsonArray
        )
    }
}