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

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


import org.envirocar.core.entity.Phenomenon;
import org.envirocar.core.entity.PhenomenonImpl;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.entity.UserStatisticsImpl;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Class that deserializes the user statistics of
 * <code>{
 * "statistics": [
 * {
 * "max": 100,
 * "avg": 61.55097170132969,
 * "min": 20,
 * "measurements": 5866,
 * "tracks": 15,
 * "users": 1,
 * "sensors": 6,
 * "phenomenon": {
 * "name": "Intake Pressure",
 * "unit": "kPa"
 * }
 * }, ...
 * </code>
 *
 * @author dewall
 */
public class UserStatisticDeserializer implements JsonDeserializer<UserStatistics> {

    @Override
    public UserStatistics deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        final JsonArray statistics = json.getAsJsonObject()
                .get(User.KEY_STATISTICS)
                .getAsJsonArray();

        Map<String, Phenomenon> statisticMap = Maps.newHashMap();

        // Iterate through the array of phenomenons and its values.
        for (int i = 0; i < statistics.size(); i++) {
            // Create a new phenomenon holder.
            Phenomenon holder = new PhenomenonImpl();

            // Get the values of the phenomenon.
            JsonObject statisticObject = statistics.get(i).getAsJsonObject();
            holder.setMaxValue(statisticObject.get(User.KEY_STATISTICS_MAX).getAsDouble());
            holder.setMinValue(statisticObject.get(User.KEY_STATISTICS_MIN).getAsDouble());
            holder.setAvgValue(statisticObject.get(User.KEY_STATISTICS_AVG).getAsDouble());

            // Get the phenomenon name and unit.
            JsonObject phenomenonObject = statisticObject.get(User
                    .KEY_STATISTICS_PHENOMENON).getAsJsonObject();
            holder.setPhenomenonName(phenomenonObject.get(User
                    .KEY_STATISTICS_PHENOMENON_NAME).getAsString());
            holder.setPhenomenonUnit(phenomenonObject.get(User
                    .KEY_STATISTICS_PHENOMENON_UNIT).getAsString());

            // Add the phenomenon to the map.
            statisticMap.put(holder.getPhenomenonName(), holder);
        }

        UserStatistics userStatistics = new UserStatisticsImpl();
        userStatistics.setStatistics(statisticMap);

        return userStatistics;
    }
}
