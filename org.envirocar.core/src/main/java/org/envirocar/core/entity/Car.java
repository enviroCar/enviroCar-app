/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.entity;

import org.envirocar.core.R;

import java.io.Serializable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Car extends BaseEntity<Car>, Serializable {
    String KEY_ROOT = "sensors";
    String KEY_CAR = "properties";
    String KEY_CAR_TYPE = "type";
    String KEY_CAR_MODEL = "model";
    String KEY_CAR_ID = "id";
    String KEY_CAR_FUELTYPE = "fuelType";
    String KEY_CAR_CONSTRUCTIONYEAR = "constructionYear";
    String KEY_CAR_MANUFACTURER = "manufacturer";
    String KEY_CAR_ENGINEDISPLACEMENT = "engineDisplacement";

    String KEY_FUELTYPE_ENUM_GASOLINE = "gasoline";
    String KEY_FUELTYPE_ENUM_DIESEL = "diesel";

    String TEMPORARY_SENSOR_ID = "%TMP_ID%";

    // fuel type strings
    String FUELTYPE_GASOLINE = "gasoline";
    String FUELTYPE_DIESEL = "diesel";
    String FUELTYPE_GAS = "gas";
    String FUELTYPE_HYBRID = "hybrid";
    String FUELTYPE_ELECTRIC = "electric";

    interface FuelTypeStrings {
        int getStringResource();
    }

    enum FuelType implements FuelTypeStrings {
        GASOLINE {
            @Override
            public int getStringResource() {
                return R.string.fuel_type_gasoline;
            }

            public String toString() {
                return FUELTYPE_GASOLINE;
            }

        },
        DIESEL {
            @Override
            public int getStringResource() {
                return R.string.fuel_type_diesel;
            }

            public String toString() {
                return FUELTYPE_DIESEL;
            }
        },
        GAS {
            @Override
            public int getStringResource() {
                return R.string.fuel_type_gas;
            }

            @Override
            public String toString() {
                return FUELTYPE_GAS;
            }
        },
        HYBRID {
            @Override
            public int getStringResource() {
                return R.string.fuel_type_hybrid;
            }

            @Override
            public String toString() {
                return FUELTYPE_HYBRID;
            }
        },
        ELECTRIC {
            @Override
            public int getStringResource() {
                return R.string.fuel_type_electric;
            }

            @Override
            public String toString() {
                return FUELTYPE_ELECTRIC;
            }
        };

        public static FuelType resolveFuelType(String fuelType) {
            if (fuelType.equals(GASOLINE.toString())) {
                return GASOLINE;
            } else if (fuelType.equals(DIESEL.toString())) {
                return DIESEL;
            }
            return null;
        }
    }


    String getId();

    void setId(String id);

    String getManufacturer();

    void setManufacturer(String manufacturer);

    String getModel();

    void setModel(String model);

    int getConstructionYear();

    void setConstructionYear(int constructionYear);

    int getEngineDisplacement();

    void setEngineDisplacement(int engineDisplacement);

    FuelType getFuelType();

    void setFuelType(FuelType fuelType);

    void setFuelType(String fuelType);

}
