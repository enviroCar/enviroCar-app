/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.remote.serde;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.logging.Logger;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class CarSerde implements JsonSerializer<Car>, JsonDeserializer<Car> {
    private static final Logger LOG = Logger.getLogger(CarSerde.class);

    @Override
    public JsonElement serialize(Car src, Type typeOfSrc, JsonSerializationContext context) {

        // set the properties of the json object
        JsonObject carProperties = new JsonObject();
        carProperties.addProperty(Car.KEY_CAR_MANUFACTURER, src.getManufacturer());
        carProperties.addProperty(Car.KEY_CAR_MODEL, src.getModel());
        carProperties.addProperty(Car.KEY_CAR_FUELTYPE, src.getFuelType().toString());
        carProperties.addProperty(Car.KEY_CAR_CONSTRUCTIONYEAR,
                src.getConstructionYear());
        carProperties.addProperty(Car.KEY_CAR_ENGINEDISPLACEMENT,
                src.getEngineDisplacement());

        // define the complete json object including type and properties.
        JsonObject carObject = new JsonObject();
        carObject.addProperty(Car.KEY_CAR_TYPE, "car");
        carObject.add(Car.KEY_CAR, carProperties);

        return carObject;
    }

    @Override
    public Car deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {
        // If the element is no car element, then return null.
        if (!json.getAsJsonObject().get(Car.KEY_CAR_TYPE).getAsString().equals("car")) {
            return null;
        }

        // Get the "sensors" element from the parsed JSON
        JsonObject car = json.getAsJsonObject().get("properties").getAsJsonObject();

        // Deserialize it.
        Car res = new CarImpl();
        res.setId(car.get(Car.KEY_CAR_ID).getAsString());
        res.setManufacturer(car.get(Car.KEY_CAR_MANUFACTURER).getAsString());
        res.setModel(car.get(Car.KEY_CAR_MODEL).getAsString());
        if (car.has(Car.KEY_CAR_ENGINEDISPLACEMENT))
            res.setEngineDisplacement(car.get(Car.KEY_CAR_ENGINEDISPLACEMENT).getAsInt());
        res.setConstructionYear(car.get(Car.KEY_CAR_CONSTRUCTIONYEAR).getAsInt());
        res.setFuelType(car.get(Car.KEY_CAR_FUELTYPE).getAsString());

        // Check whether the result has been correctly generated and log it in case when not.
        return res;
    }
}