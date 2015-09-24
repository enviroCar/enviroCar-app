package org.envirocar.app.model.dao.service.serializer;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.service.CarService;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class CarSerializer implements JsonSerializer<Car>, JsonDeserializer<Car> {
    private static final Logger LOG = Logger.getLogger(CarSerializer.class);

    @Override
    public JsonElement serialize(Car src, Type typeOfSrc, JsonSerializationContext context) {

        // set the properties of the json object
        JsonObject carProperties = new JsonObject();
        carProperties.addProperty(CarService.KEY_CAR_MANUFACTURER, src.getManufacturer());
        carProperties.addProperty(CarService.KEY_CAR_MODEL, src.getModel());
        carProperties.addProperty(CarService.KEY_CAR_FUELTYPE, src.getFuelType().toString());
        carProperties.addProperty(CarService.KEY_CAR_CONSTRUCTIONYEAR,
                src.getConstructionYear());
        carProperties.addProperty(CarService.KEY_CAR_ENGINEDISPLACEMENT,
                src.getEngineDisplacement());

        // define the complete json object including type and properties.
        JsonObject carObject = new JsonObject();
        carObject.addProperty(CarService.KEY_CAR_TYPE, "car");
        carObject.add(CarService.KEY_CAR, carProperties);

        return carObject;
    }

    @Override
    public Car deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {
        // If the element is no car element, then return null.
        if(!json.getAsJsonObject().get(CarService.KEY_CAR_TYPE).getAsString().equals("car")){
            return null;
        }

        // Get the "sensors" element from the parsed JSON
        JsonElement car = json.getAsJsonObject().get("properties");

        // Deserialize it.
        Car res = new Gson().fromJson(car, Car.class);

        // Check whether the result has been correctly generated and log it in case when not.
        if(res == null){
            LOG.severe("Error while parsing car object. No valid json format: "
                    + car.getAsString());
        }
        return res;
    }
}