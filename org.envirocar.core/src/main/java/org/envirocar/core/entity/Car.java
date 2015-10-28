package org.envirocar.core.entity;

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

    String FUELTYPE_GASOLINE = "gasoline";
    String FUELTYPE_DIESEL = "diesel";

    String TEMPORARY_SENSOR_ID = "%TMP_ID%";

    enum FuelType {
        GASOLINE {
            public String toString() {
                return FUELTYPE_GASOLINE;
            }

        },
        DIESEL {
            public String toString() {
                return FUELTYPE_DIESEL;
            }
        };

        public static FuelType resolveFuelType(String fuelType){
            if(fuelType.equals(GASOLINE.toString())){
                return GASOLINE;
            } else if(fuelType.equals(DIESEL.toString())){
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
