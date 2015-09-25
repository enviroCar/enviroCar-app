package org.envirocar.app.model.dao.service.serializer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.model.dao.service.TermsOfUseService;

import java.lang.reflect.Type;
import java.util.List;

/**
 * JSON serializer for the terms of use responses.
 *
 * @author dewall
 */
public class TermsOfUseSerializer implements JsonDeserializer<TermsOfUse> {
    @Override
    public TermsOfUse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        // Get the root element of the terms of use array.
        JsonArray termsOfUseArray = json.getAsJsonObject()
                .get(TermsOfUseService.KEY_TERMSOFUSE)
                .getAsJsonArray();

        // Get all the instances of the terms of use.
        List<TermsOfUseInstance> instances = Lists.newArrayList();
        for (int i = 0; i < termsOfUseArray.size(); i++) {
            // Get the values of the terms of use.
            JsonObject termsOfUseObject = termsOfUseArray.get(i).getAsJsonObject();
            String id = termsOfUseObject.get(TermsOfUseService.KEY_TERMSOFUSE_ID).getAsString();
            String issuedDate = termsOfUseObject.get(TermsOfUseService.KEY_TERMSOFUSE_ISSUEDDATE)
                    .getAsString();

            // add the terms of use instance to the arraylist.
            instances.add(new TermsOfUseInstance(id, issuedDate));
        }

        // Return a terms of use holder that holds all the intances of the terms of use.
        return new TermsOfUse(instances);
    }
}
