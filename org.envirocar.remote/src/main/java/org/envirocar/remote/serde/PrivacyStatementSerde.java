/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.remote.serde;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.PrivacyStatementImpl;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author dewall
 */
@Singleton
public class PrivacyStatementSerde implements JsonDeserializer<PrivacyStatement> {

    @Inject
    public PrivacyStatementSerde() {
    }

    @Override
    public PrivacyStatement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Get the values of the privacy statement.
        JsonObject termsOfUseObject = json.getAsJsonObject();
        String id = termsOfUseObject.get(PrivacyStatement.KEY_PRIVACY_STATEMENT_ID).getAsString();
        String issuedDate = termsOfUseObject.get(PrivacyStatement.KEY_PRIVACY_STATEMENT_ISSUEDATE).getAsString();

        // Get the optional content.
        String content = null;
        if (termsOfUseObject.has(PrivacyStatement.KEY_PRIVACY_STATEMENT_CONTENTS)) {
            content = termsOfUseObject.get(PrivacyStatement.KEY_PRIVACY_STATEMENT_CONTENTS).getAsString();
        }

        return new PrivacyStatementImpl(id, issuedDate, content);
    }
}
