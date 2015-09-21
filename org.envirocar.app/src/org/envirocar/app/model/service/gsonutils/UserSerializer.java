package org.envirocar.app.model.service.gsonutils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.app.model.User;
import org.envirocar.app.model.service.UserService;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class UserSerializer implements JsonSerializer<User> {

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject user = new JsonObject();
        user.addProperty(UserService.KEY_USER_NAME, src.getUsername());
        user.addProperty(UserService.KEY_USER_TOKEN, src.getToken());

        if (src.getMail() != null) {
            user.addProperty(UserService.KEY_USER_MAIL, src.getMail());
        }

        if (src.getTouVersion() != null) {
            user.addProperty(UserService.KEY_USER_TOU_VERSION, src.getTouVersion());
        }

        return user;
    }
}
