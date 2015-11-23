/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.remote.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.TermsOfUse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TermsOfUseListSerializer implements JsonDeserializer<List<TermsOfUse>> {

    @Override
    public List<TermsOfUse> deserialize(JsonElement json, Type typeOfT,
                                        JsonDeserializationContext context)
            throws JsonParseException {
        // Get the json element as array
        JsonArray array = json.getAsJsonObject().get(TermsOfUse.KEY_TERMSOFUSE).getAsJsonArray();

        // Iterate over each json object
        List<TermsOfUse> res = new ArrayList<>(array.size());
        for(int i = 0, size = array.size(); i < size; i++){
            // and deserialize the json object.
            res.add(context.deserialize(array.get(i), TermsOfUse.class));
        }

        return res;
    }
}
