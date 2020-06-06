package org.envirocar.storage;

import androidx.room.Database;
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

}
