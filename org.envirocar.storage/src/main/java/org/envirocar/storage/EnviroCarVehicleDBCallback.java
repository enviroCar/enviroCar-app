package org.envirocar.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.utils.DataGenerator;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class EnviroCarVehicleDBCallback extends RoomDatabase.Callback {

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;

    Context context;

    EnviroCarVehicleDBCallback(Context context) {
        this.context = context;
    }

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
}
