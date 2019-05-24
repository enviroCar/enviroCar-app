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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

/**
 * @author dewall
 */
public class MeasurementSerializer implements JsonSerializer<Measurement>,
        JsonDeserializer<Measurement> {
    private static final Logger LOG = Logger.getLogger(MeasurementSerializer.class);

    @Override
    public JsonElement serialize(Measurement src, Type typeOfSrc, JsonSerializationContext
            context) {
        return null;
    }

    @Override
    public Measurement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Get the coordinates of the measurement
        JsonArray coords = jsonObject.getAsJsonObject()
                .get(Track.KEY_TRACK_FEATURES_GEOMETRY).getAsJsonObject()
                .get(Track.KEY_TRACK_FEATURES_GEOMETRY_COORDINATES).getAsJsonArray();

        Measurement result = new MeasurementImpl();
        result.setLatitude(coords.get(1).getAsFloat());
        result.setLongitude(coords.get(0).getAsFloat());

        // Get the properties of the measurement
        JsonObject propertiesObject = jsonObject.getAsJsonObject(
                Track.KEY_TRACK_FEATURES_PROPERTIES);
        try {
            // Parse the time.
            result.setTime(Util.isoDateToLong(propertiesObject.get(
                    Track.KEY_TRACK_FEATURES_PROPERTIES_TIME).getAsString()));
        } catch (ParseException e) {
            new JsonParseException(e);
        }

        // Get all the phenomenons and its measured values.
        JsonObject phenomenonsObject = propertiesObject.get(Track
                .KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = phenomenonsObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            Double value = entry.getValue().getAsJsonObject().get(Track
                    .KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_VALUE).getAsDouble();
            result.setProperty(Measurement.PropertyKeyValues.get(entry.getKey()), value);
        }

        return result;
    }
}
