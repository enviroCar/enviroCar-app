package org.envirocar.remote.serde;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.UserStatisticImpl;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class UserStatisticSerde implements JsonDeserializer<UserStatistic> {

    @Override
    public UserStatistic deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        int trackCount = jsonObject.get(UserStatistic.KEY_TRACKCOUNT).getAsInt();
        double distance = jsonObject.get(UserStatistic.KEY_DISTANCE).getAsDouble();
        double duration = jsonObject.get(UserStatistic.KEY_DURATION).getAsDouble();

        return new UserStatisticImpl(trackCount, distance, duration);
    }
}
