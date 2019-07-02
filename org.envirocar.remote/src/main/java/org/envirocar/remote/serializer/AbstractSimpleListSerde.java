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
