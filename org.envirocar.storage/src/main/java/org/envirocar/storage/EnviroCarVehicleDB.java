package org.envirocar.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.dao.LocalManufacturersDAO;
import org.envirocar.storage.dao.LocalPowerSourcesDAO;
import org.envirocar.storage.dao.LocalVehicleDAO;

@Database(entities = {Manufacturers.class, Vehicles.class, PowerSource.class}, version = 1)
public abstract class EnviroCarVehicleDB extends RoomDatabase {

    //DAO car selection
    public abstract LocalManufacturersDAO manufacturersDAO();
    public abstract LocalPowerSourcesDAO powerSourcesDAO();
    public abstract LocalVehicleDAO vehicleDAO();

    private static EnviroCarVehicleDB enviroCarVehicleDB;

    public static EnviroCarVehicleDB getInstance(Context context) {
        if (enviroCarVehicleDB == null) {
            enviroCarVehicleDB =  Room.databaseBuilder(context,EnviroCarVehicleDB.class,"Samew.db")
                    .addCallback(new EnviroCarVehicleDBCallback(context,enviroCarVehicleDB))
                    .build();
        }
        return enviroCarVehicleDB;
    }
}
