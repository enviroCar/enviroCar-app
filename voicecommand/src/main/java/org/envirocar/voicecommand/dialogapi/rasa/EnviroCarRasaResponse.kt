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

import com.google.gson.JsonArray
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply
import org.envirocar.voicecommand.model.CustomRasaResponse

/**
 * @author Dhiraj Chauhan
 */

data class EnviroCarRasaResponse(
    override var query: String?,
    val recipient_id: String = "",
    val text: String? = null,
    override var action: String? = null,
    var actionType: String? = null,
    override val intent: String? = null,
    override val question: Boolean? = true,
    override val replies: List<Reply> = listOf(),
    var custom: CustomRasaResponse? = null,
    val httpResponse: JsonArray? = null,
) : Response