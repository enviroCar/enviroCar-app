package org.envirocar.app.model.service.gsonutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.envirocar.app.model.Car;
import org.envirocar.app.model.service.CarService;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * GSON Json Deserializer that deserializes response of "sensors" in the following format.
 * Requires an additional {@link CarSerializer} because of the "properties" nested object.
 *
 * {
 * "sensors": [
 * {
 * "type": "car",
 * "properties": {
 * "model": "R 1200 GS ADV",
 * "id": "51c96afce4b0fd063432096f",
 * "fuelType": "gasoline",
 * "constructionYear": 2012,
 * "manufacturer": "BMW"
 * }
 * },
 * {
 * ...
 *w
 * @author dewall
 */
public class CarListDeserializer implements JsonDeserializer<List<Car>> {

    @Override
    public List<Car> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {
        // Get the "sensors" element from the parsed JSON
        final JsonArray sensors = json.getAsJsonObject()
                .get(CarService.KEY_ROOT)
                .getAsJsonArray();

        List<Car> result = new ArrayList<Car>();
        for (int i = 0; i < sensors.size(); i++) {
            result.add(context.<Car>deserialize(sensors.get(i), Car.class));
        }

        return result;
    }
}
