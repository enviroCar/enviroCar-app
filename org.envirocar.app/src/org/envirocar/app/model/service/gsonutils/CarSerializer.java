package org.envirocar.app.model.service.gsonutils;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.app.model.Car;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class CarSerializer implements JsonSerializer<Car>, JsonDeserializer<Car> {

    @Override
    public JsonElement serialize(Car src, Type typeOfSrc, JsonSerializationContext context) {

//        // set the properties of the json object
//        JsonObject carProperties = new JsonObject();
//        carProperties.addProperty(CarService.KEY_CAR_MANUFACTURER, src.manufacturer);
//        carProperties.addProperty(CarService.KEY_CAR_MODEL, src.model);
//        carProperties.addProperty(CarService.KEY_CAR_FUELTYPE, src.fuelType);
//        carProperties.addProperty(CarService.KEY_CAR_CONSTRUCTIONYEAR,
//                Integer.parseInt(src.constructionYear));
//        carProperties.addProperty(CarService.KEY_CAR_ENGINEDISPLACEMENT,
//                Integer.parseInt(src.engineDisplacement));
//
//        // define the json object
//        JsonObject carObject = new JsonObject();
//        carObject.addProperty(CarService.KEY_CAR_TYPE, "car");
//        carObject.add(CarService.KEY_CAR, carProperties);
//
//        return carObject;
        return null;
    }

    @Override
    public Car deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        // Get the "sensors" element from the parsed JSON
        JsonElement car = json.getAsJsonObject().get("properties");

        // Deserialize it.
        return new Gson().fromJson(car, Car.class);
    }
}