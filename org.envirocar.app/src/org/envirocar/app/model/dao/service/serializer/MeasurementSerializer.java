package org.envirocar.app.model.dao.service.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.dao.service.TrackService;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.util.Util;

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
                .get(TrackService.KEY_TRACK_FEATURES_GEOMETRY).getAsJsonObject()
                .get(TrackService.KEY_TRACK_FEATURES_GEOMETRY_COORDINATES).getAsJsonArray();
        Measurement result = new Measurement(
                coords.get(1).getAsFloat(),
                coords.get(0).getAsFloat());

        // Get the properties of the measurement
        JsonObject propertiesObject = jsonObject.getAsJsonObject(
                TrackService.KEY_TRACK_FEATURES_PROPERTIES);
        try {
            // Parse the time.
            result.setTime(Util.isoDateToLong(propertiesObject.get(
                    TrackService.KEY_TRACK_FEATURES_PROPERTIES_TIME).getAsString()));
        } catch (ParseException e) {
            new JsonParseException(e);
        }

        // Get all the phenomenons and its measured values.
        JsonObject phenomenonsObject = propertiesObject.get(TrackService
                .KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = phenomenonsObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            Double value = entry.getValue().getAsJsonObject().get(TrackService
                    .KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_VALUE).getAsDouble();
            result.setProperty(Measurement.PropertyKeyValues.get(entry.getKey()), value);
        }

        return result;
    }
}
