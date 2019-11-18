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
package org.envirocar.remote.serde;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.UserStatisticImpl;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class UserStatisticSerde implements JsonDeserializer<UserStatistic> {

    @Override
    public UserStatistic deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int trackCount = 0;
        if (jsonObject.has(UserStatistic.KEY_TRACKCOUNT)) {
            trackCount = jsonObject.get(UserStatistic.KEY_TRACKCOUNT).getAsInt();
        }
        double distance = jsonObject.get(UserStatistic.KEY_DISTANCE).getAsDouble();
        double duration = jsonObject.get(UserStatistic.KEY_DURATION).getAsDouble() * 60 * 60 * 1000;

        return new UserStatisticImpl(trackCount, distance, duration);
    }
}
