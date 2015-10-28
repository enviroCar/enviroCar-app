package org.envirocar.remote.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class UserSerializer implements JsonSerializer<User>, JsonDeserializer<User> {

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject user = new JsonObject();
        if (src.getUsername() != null) {
            user.addProperty(User.KEY_USER_NAME, src.getUsername());
        }

        if (src.getToken() != null) {
            user.addProperty(User.KEY_USER_TOKEN, src.getToken());
        }

        if (src.getMail() != null) {
            user.addProperty(User.KEY_USER_MAIL, src.getMail());
        }

        if (src.getTermsOfUseVersion() != null) {
            user.addProperty(User.KEY_USER_TOU_VERSION, src.getTermsOfUseVersion());
        }

        return user;
    }

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject userObject = json.getAsJsonObject();
        String username = userObject.get(User.KEY_USER_NAME).getAsString();
        String mail = userObject.get(User.KEY_USER_MAIL).getAsString();

        User user = new UserImpl();
        user.setUsername(username);
        user.setMail(mail);

        if (userObject.has(User.KEY_USER_TOU_VERSION)) {
            String touVersion = userObject.get(User.KEY_USER_TOU_VERSION).getAsString();
            user.setTermsOfUseVersion(touVersion);
        }

        return user;
    }
}
