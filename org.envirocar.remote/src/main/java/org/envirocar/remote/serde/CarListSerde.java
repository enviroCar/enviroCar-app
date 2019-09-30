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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.Car;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * GSON Json Deserializer that deserializes response of "sensors" in the following format.
 * Requires an additional {@link CarSerde} because of the "properties" nested object.
 * <p/>
 * {
 * "sensors": [
 * {
 * "type": "car",
 * "properties": {
 * "model": "R 1200 GS ADV",
 * "id": "51c96afce4b0fd063432096f",
 * "fuelType": "gasoline",
 * "constructionYear": 2012,
 * "manufacturer": "BMW"
 * }
 * },
 * {
 * ...
 *
 * @author dewall
 */
public class CarListSerde implements JsonDeserializer<List<Car>> {

    @Override
    public List<Car> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {
        // Get the "sensors" element from the parsed JSON
        final JsonArray sensors = json.getAsJsonObject()
                .get(Car.KEY_ROOT).getAsJsonArray();

        List<Car> result = new ArrayList<Car>();
        for (int i = 0; i < sensors.size(); i++) {
            result.add(context.deserialize(sensors.get(i), Car.class));
        }

        return result;
    }
}
