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
package org.envirocar.core.entity;

import android.content.Context;

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
    String KEY_CAR_WEIGHT = "weight";
    String KEY_CAR_VEHICLETYPE = "vehicleType";
    String KEY_CAR_EMISSION_CLASS = "emissionClass";
    

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
            } else if (fuelType.equals(GAS.toString())) {
                return GAS;
            } else if (fuelType.equals(ELECTRIC.toString())) {
                return ELECTRIC;
            } else if (fuelType.equals(HYBRID.toString())) {
                return HYBRID;
            }
            else
            return null;
        }

        public static Car.FuelType getFuelTybeByTranslatedString(Context context, String fueltype) {
            for (Car.FuelType fuelType : Car.FuelType.values()) {
                if (context.getString(fuelType.getStringResource()).equals(fueltype)) {
                    return fuelType;
                }
            }
            return null;
        }
    }

    String VEHICLETYPE_PASSENGER = "Passenger Car";
    String VEHICLETYPE_UTILITY = "Utility Car";
    String VEHICLETYPE_TAXI = "Taxi";
    
    interface VehicleTypeStrings {
        int getStringResource();
    }

    enum VehicleType implements VehicleTypeStrings {
        PASSENGER {
            @Override
            public int getStringResource() {
                return R.string.car_selection_private_vehicle;
            }

            public String toString() {
                return VEHICLETYPE_PASSENGER;
            }
        },
        UTILITY {
            @Override
            public int getStringResource() {
                return R.string.car_selection_utility_car;
            }

            public String toString() {
                return VEHICLETYPE_UTILITY;
            }
            
        },
        TAXI {
            @Override
            public int getStringResource() {
                return R.string.car_selection_taxi;
            }

            public String toString() {
                return VEHICLETYPE_TAXI;
            }   
        };

        public static VehicleType resolveVehicleType(String vehicleType) {
            if (vehicleType == null) {
                return null;
            }
            
            if (vehicleType.equals(PASSENGER.toString())) {
                return PASSENGER;
            } else if (vehicleType.equals(UTILITY.toString())) {
                return UTILITY;
            } else if (vehicleType.equals(TAXI.toString())) {
                return TAXI;
            }
            
            return null;
        }
    }

    String EMISSION_CLASS_EURO1 = "Euro 1";
    String EMISSION_CLASS_EURO2 = "Euro 2";
    String EMISSION_CLASS_EURO3 = "Euro 3";
    String EMISSION_CLASS_EURO4 = "Euro 4";
    String EMISSION_CLASS_EURO5A = "Euro 5a";
    String EMISSION_CLASS_EURO5B = "Euro 5b";
    String EMISSION_CLASS_EURO6B = "Euro 6b";
    String EMISSION_CLASS_EURO6C = "Euro 6c";
    String EMISSION_CLASS_EURO6DTEMP = "Euro 6d-TEMP";
    String EMISSION_CLASS_EURO6D = "Euro 6d";
    String EMISSION_CLASS_EURO7 = "Euro 7";
    
    
    interface EmissionClassStrings {
        int getStringResource();
    }

    enum EmissionClass implements EmissionClassStrings {
        EURO1 {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro1;
            }

            public String toString() {
                return EMISSION_CLASS_EURO1;
            }
        },
        EURO2 {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro2;
            }

            public String toString() {
                return EMISSION_CLASS_EURO2;
            }
        },
        EURO3 {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro3;
            }

            public String toString() {
                return EMISSION_CLASS_EURO3;
            }
        },
        EURO4 {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro4;
            }

            public String toString() {
                return EMISSION_CLASS_EURO4;
            }
        },
        EURO5A {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro5a;
            }

            public String toString() {
                return EMISSION_CLASS_EURO5A;
            }
        },
        EURO5B {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro5b;
            }

            public String toString() {
                return EMISSION_CLASS_EURO5B;
            }
        },
        EURO6B {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro6b;
            }

            public String toString() {
                return EMISSION_CLASS_EURO6B;
            }
        },
        EURO6C {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro6c;
            }

            public String toString() {
                return EMISSION_CLASS_EURO6C;
            }
        },
        EURO6DTEMP {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro6d_temp;
            }

            public String toString() {
                return EMISSION_CLASS_EURO6DTEMP;
            }
        },
        EURO6D {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro6d;
            }

            public String toString() {
                return EMISSION_CLASS_EURO6D;
            }
        },
        EURO7 {
            @Override
            public int getStringResource() {
                return R.string.car_selection_emission_euro7;
            }

            public String toString() {
                return EMISSION_CLASS_EURO7;
            }
        },
        ;

        public static EmissionClass resolveEmissionClass(String emissionClass) {
            if (emissionClass == null) {
                return null;
            }
            
            if (emissionClass.equals(EURO1.toString())) {
                return EURO1;
            } else if (emissionClass.equals(EURO2.toString())) {
                return EURO2;
            } else if (emissionClass.equals(EURO3.toString())) {
                return EURO3;
            } else if (emissionClass.equals(EURO4.toString())) {
                return EURO4;
            } else if (emissionClass.equals(EURO5A.toString())) {
                return EURO5A;
            } else if (emissionClass.equals(EURO5B.toString())) {
                return EURO5B;
            } else if (emissionClass.equals(EURO6B.toString())) {
                return EURO6B;
            } else if (emissionClass.equals(EURO6C.toString())) {
                return EURO6C;
            } else if (emissionClass.equals(EURO6DTEMP.toString())) {
                return EURO6DTEMP;
            } else if (emissionClass.equals(EURO6D.toString())) {
                return EURO6D;
            } else if (emissionClass.equals(EURO7.toString())) {
                return EURO7;
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

    boolean hasEngineDispalcement();

    int getEngineDisplacement();

    void setEngineDisplacement(int engineDisplacement);

    FuelType getFuelType();

    void setFuelType(FuelType fuelType);

    void setFuelType(String fuelType);

    boolean hasWeight();

    int getWeight();

    void setWeight(int weight);

    VehicleType getVehicleType();

    void setVehicleType(String vehicleType);

    void setVehicleType(VehicleType vehicleType);

    EmissionClass getEmissionClass();

    void setEmissionClass(String emissionClass);

    void setEmissionClass(EmissionClass emissionClass);

}
