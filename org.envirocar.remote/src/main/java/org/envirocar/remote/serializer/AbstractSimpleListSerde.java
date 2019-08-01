/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.BaseEntity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSimpleListSerde<T extends BaseEntity> implements JsonSerializer<List<T>>, JsonDeserializer<List<T>> {

    private final String rootKey;
    private final Class<T> entityClass;

    /**
     * Constructor.
     *
     * @param rootKey     the root key in the json containing the list
     * @param entityClass the entity class to parse the json for
     */
    public AbstractSimpleListSerde(String rootKey, Class<T> entityClass) {
        this.rootKey = rootKey;
        this.entityClass = entityClass;
    }

    @Override
    public JsonElement serialize(List<T> src, Type typeOfSrc, JsonSerializationContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Get the json element as array
        JsonArray array = json.getAsJsonObject().get(rootKey).getAsJsonArray();

        List<T> res = new ArrayList<>(array.size());
        for (int i = 0, size = array.size(); i < size; i++) {
            // and deserialize the json object
            res.add(context.deserialize(array.get(i), this.entityClass));
        }

        return res;
    }

}
