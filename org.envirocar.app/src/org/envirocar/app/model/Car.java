/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.model;

import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Class holding all information for a car instance
 *
 * @author matthes rieke
 */
public class Car implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6321429785990500936L;
    private static final Logger logger = Logger.getLogger(Car.class);
    private static final String GASOLINE_STRING = "gasoline";
    private static final String DIESEL_STRING = "diesel";

    public static final String TEMPORARY_SENSOR_ID = "%TMP_ID%";

    public enum FuelType {
        GASOLINE {
            public String toString() {
                return GASOLINE_STRING;
            }

        },
        DIESEL {
            public String toString() {
                return DIESEL_STRING;
            }
        }

    }

    private FuelType fuelType;
    private String manufacturer;
    private String model;
    private String id;
    private int constructionYear;
    private int engineDisplacement;

    public Car(FuelType fuelType, String manufacturer, String model, String id, int year, int
            engineDisplacement) {
        this.fuelType = fuelType;
        this.manufacturer = manufacturer;
        this.model = model;
        this.id = id;
        this.constructionYear = year;
        this.engineDisplacement = engineDisplacement;
    }

    private Car(String fuelType, String manufacturer, String model, String id, int year, int
            engineDisplacement) {
        this(resolveFuelType(fuelType), manufacturer, model, id, year, engineDisplacement);
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String newID) {
        this.id = newID;
    }

    public int getConstructionYear() {
        return constructionYear;
    }

    public void setConstructionYear(int year) {
        this.constructionYear = year;
    }

    /**
     * @return the engine displacement in cubic centimeters
     */
    public int getEngineDisplacement() {
        return engineDisplacement;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(manufacturer);
        sb.append(" ");
        sb.append(model);
        sb.append(" ");
        sb.append(constructionYear);
        sb.append(" (");
        sb.append(fuelType);
        sb.append(" / ");
        sb.append(engineDisplacement);
        sb.append("cc)");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Car) {
            Car c = (Car) o;
            result = this.fuelType == c.fuelType
                    && this.manufacturer.equals(c.manufacturer)
                    && this.model.equals(c.model)
                    && this.id.equals(c.id)
                    && this.constructionYear == c.constructionYear
                    && this.engineDisplacement == c.engineDisplacement;
        }
        return result;
    }

    public static Car fromJsonWithStrictEngineDisplacement(JSONObject jsonObject) throws
            JSONException {
        String manu = jsonObject.getString("manufacturer");
        String modl = jsonObject.getString("model");
        String foolType = jsonObject.getString("fuelType");
        int decon = jsonObject.getInt("constructionYear");
        String eyeD = jsonObject.getString("id");

        int engineDiss;
        try {
            engineDiss = jsonObject.getInt("engineDisplacement");
        } catch (JSONException e) {
            throw e;
        }

        return new Car(foolType, manu, modl, eyeD, decon, engineDiss);
    }

    public static Car fromJson(JSONObject jsonObject) throws JSONException {
        String manu = jsonObject.getString("manufacturer");
        String modl = jsonObject.getString("model");
        String foolType = jsonObject.getString("fuelType");
        int decon = jsonObject.getInt("constructionYear");
        String eyeD = jsonObject.getString("id");

        int engineDiss = jsonObject.optInt("engineDisplacement", 2000);

        return new Car(foolType, manu, modl, eyeD, decon, engineDiss);
    }

    public static List<Car> fromJsonList(JSONObject json) throws JSONException {
        JSONArray cars = json.getJSONArray("sensors");

        List<Car> sensors = new ArrayList<Car>(cars.length());

        for (int i = 0; i < cars.length(); i++) {
            String typeString;
            JSONObject properties;
            String carId;
            try {
                typeString = ((JSONObject) cars.get(i)).optString("type", "none");
                properties = ((JSONObject) cars.get(i)).getJSONObject("properties");
                carId = properties.getString("id");
            } catch (JSONException e) {
                logger.warn(e.getMessage(), e);
                continue;
            }
            if (typeString.equals(CarSelectionPreference.SENSOR_TYPE)) {
                try {
                    sensors.add(Car.fromJsonWithStrictEngineDisplacement(properties));
                } catch (JSONException e) {
                    logger.verbose(String.format("Car '%s' not supported: %s", carId != null ?
                            carId : "null", e.getMessage()));
                }
            }
        }

        return sensors;
    }

    public static Observable<Car> observableFromJson(JSONObject json) throws JSONException {
        final JSONArray cars = json.getJSONArray("sensors");

        return Observable.create(new Observable.OnSubscribe<Car>() {
            @Override
            public void call(Subscriber<? super Car> subscriber) {
                for (int i = 0, size = cars.length(); i < size; i++) {
                    String typeString;
                    JSONObject properties;
                    String carId;
                    try {
                        typeString = ((JSONObject) cars.get(i)).optString("type", "none");
                        properties = ((JSONObject) cars.get(i)).getJSONObject("properties");
                        carId = properties.getString("id");
                    } catch (JSONException e) {
                        logger.warn(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    }
                    if (typeString.equals(CarSelectionPreference.SENSOR_TYPE)) {
                        try {
                            Car car = Car.fromJsonWithStrictEngineDisplacement(properties);
                            subscriber.onNext(car);
                        } catch (JSONException e) {
                            subscriber.onError(e);
                            logger.verbose(String.format("Car '%s' not supported: %s", carId != null ? carId : "null", e.getMessage()));
                        }
                    }
                }
            }
        });
    }


    public static FuelType resolveFuelType(String foolType) {
        if (foolType.equals(GASOLINE_STRING)) {
            return FuelType.GASOLINE;
        } else if (foolType.equals(DIESEL_STRING)) {
            return FuelType.DIESEL;
        }
        return FuelType.GASOLINE;
    }

    public static double ccmToLiter(int ccm) {
        float result = ccm / 1000.0f;
        return result;
    }

}
