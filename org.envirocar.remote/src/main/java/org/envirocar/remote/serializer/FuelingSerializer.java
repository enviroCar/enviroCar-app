package org.envirocar.remote.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


import org.envirocar.core.entity.Fueling;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class FuelingSerializer implements JsonDeserializer<Fueling> {
    @Override
    public Fueling deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // TODO clarify the structure and implement it.
        return null;
    }
}
