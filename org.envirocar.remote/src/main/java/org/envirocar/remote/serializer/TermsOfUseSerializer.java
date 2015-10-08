package org.envirocar.remote.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.TermsOfUseImpl;

import java.lang.reflect.Type;

/**
 * JSON serializer for the terms of use responses.
 *
 * @author dewall
 */
public class TermsOfUseSerializer implements JsonSerializer<TermsOfUse>,
        JsonDeserializer<TermsOfUse> {

    @Override
    public JsonElement serialize(TermsOfUse src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject result = new JsonObject();
        result.addProperty(TermsOfUse.KEY_TERMSOFUSE_ID, src.getId());
        result.addProperty(TermsOfUse.KEY_TERMSOFUSE_ISSUEDDATE, src.getIssuedDate());

        return result;
    }

    @Override
    public TermsOfUse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        //        // Get the root element of the terms of use array.
        //        JsonArray termsOfUseArray = json.getAsJsonObject()
        //                .get(TermsOfUse.KEY_TERMSOFUSE)
        //                .getAsJsonArray();
        //
        //        // Get all the instances of the terms of use.
        //        List<TermsOfUse> instances = Lists.newArrayList();
        //        for (int i = 0; i < termsOfUseArray.size(); i++) {
        //            // Get the values of the terms of use.
        //            JsonObject termsOfUseObject = termsOfUseArray.get(i).getAsJsonObject();
        //            String id = termsOfUseObject.get(TermsOfUse.KEY_TERMSOFUSE_ID).getAsString();
        //            String issuedDate = termsOfUseObject.get(TermsOfUse.KEY_TERMSOFUSE_ISSUEDDATE)
        //                    .getAsString();
        //
        //            // add the terms of use instance to the arraylist.
        //            instances.add(new TermsOfUse(id, issuedDate));
        //        }
        //
        //        // Return a terms of use holder that holds all the intances of the terms of use.
        //        return new TermsOfUse(instances);


        // Get the values of the terms of use.
        JsonObject termsOfUseObject = json.getAsJsonObject();
        String id = termsOfUseObject.get(TermsOfUse.KEY_TERMSOFUSE_ID).getAsString();
        String issuedDate = termsOfUseObject.get(TermsOfUse.KEY_TERMSOFUSE_ISSUEDDATE)
                .getAsString();

        TermsOfUse result = new TermsOfUseImpl();
        result.setId(id);
        result.setIssuedDate(issuedDate);

        return result;
    }


}
