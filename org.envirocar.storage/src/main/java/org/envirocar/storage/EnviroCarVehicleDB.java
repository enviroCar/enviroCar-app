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

    private static EnviroCarVehicleDB enviroCarVehicleDB;

    public static EnviroCarVehicleDB getInstance(Context context) {
        if (enviroCarVehicleDB == null) {
            enviroCarVehicleDB = Room.databaseBuilder(context, EnviroCarVehicleDB.class, "Samew.db")
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Executor executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                List<Vehicles> vehiclesList = DataGenerator.getVehicleData(context, "vehicles");
                                List<PowerSource> powerSourceList = DataGenerator.getPowerSources(context, "power_sources");
                                enviroCarVehicleDB.vehicleDAO().insert(vehiclesList);
                                enviroCarVehicleDB.powerSourcesDAO().insert(powerSourceList);

                            });
                        }
                    })
                    .build();
        }

        return enviroCarVehicleDB;

    }
}
