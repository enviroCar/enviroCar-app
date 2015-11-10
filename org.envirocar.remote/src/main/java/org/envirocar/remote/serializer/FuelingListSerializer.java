package org.envirocar.remote.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.Fueling;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class FuelingListSerializer implements JsonDeserializer<List<Fueling>> {
    @Override
    public List<Fueling> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        // Get the json array of fuelings.
        JsonArray fuelingArray = json.getAsJsonObject().get(Fueling.KEY_FUELINGS).getAsJsonArray();

        // Iterate through the array and create a fueling class.
        List<Fueling> result = new ArrayList<>();
        for (int i = 0; i < fuelingArray.size(); i++) {
            result.add(context.deserialize(fuelingArray.get(i), Fueling.class));
        }

        // Return the result
        return result;
    }
}
