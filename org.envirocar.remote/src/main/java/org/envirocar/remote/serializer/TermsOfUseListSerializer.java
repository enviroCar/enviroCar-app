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
