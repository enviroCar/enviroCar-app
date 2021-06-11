package org.envirocar.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.utils.DataGenerator;
import org.envirocar.storage.dao.LocalManufacturersDAO;
import org.envirocar.storage.dao.LocalPowerSourcesDAO;
import org.envirocar.storage.dao.LocalVehicleDAO;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Database(entities = {Manufacturers.class, Vehicles.class, PowerSource.class}, version = 1)
public abstract class EnviroCarVehicleDB extends RoomDatabase {

    //DAO car selection
    public abstract LocalManufacturersDAO manufacturersDAO();

    public abstract LocalPowerSourcesDAO powerSourcesDAO();

    public abstract LocalVehicleDAO vehicleDAO();
}
