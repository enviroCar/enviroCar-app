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
package org.envirocar.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.*;

import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.SqlBrite;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.DataGenerator;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module
public final class DatabaseModule {
    private static final Logger LOG = Logger.getLogger(DatabaseModule.class);

    // configs
    private static final String DATABASE_NAME = "envirocar";
    private static final String VECHILE_DATABASE_NAME = "envirocarvehicle";
    private static final int DATABASE_VERSION = 11;
    EnviroCarVehicleDB enviroCarVehicleDB;


    @Provides
    @Singleton
    SqlBrite provideSqlBrite() {
        return new SqlBrite.Builder()
                .logger(message -> LOG.info(message))
                .build();
    }

    @Provides
    @Singleton
    BriteDatabase provideBriteDatabase(@InjectApplicationScope Context context, SqlBrite sqlBrite) {
        SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(new EnviroCarDBCallback(DATABASE_VERSION))
                .build();

        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(helper, Schedulers.io());
        db.setLoggingEnabled(true);

        return db;
    }

    @Provides
    @Singleton
    EnviroCarDB provideEnvirocarDB(BriteDatabase briteDatabase) {
        return new EnviroCarDBImpl(briteDatabase);
    }

    @Provides
    @Singleton
    EnviroCarVehicleDB provideRoomDatabase(@InjectApplicationScope Context context) {
        enviroCarVehicleDB = Room.databaseBuilder(context, EnviroCarVehicleDB.class, VECHILE_DATABASE_NAME)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executor executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            List<Vehicles> vehiclesList = DataGenerator.getVehicleData(context, "vehicles");
                            List<PowerSource> powerSourceList = DataGenerator.getPowerSources(context, "power_sources");
                            enviroCarVehicleDB.vehicleDAO().insert(vehiclesList);
                            enviroCarVehicleDB.powerSourcesDAO().insert(powerSourceList);
                            enviroCarVehicleDB.manufacturersDAO().inserManufacturer();
                        });
                    }
                })
                .build();
    return enviroCarVehicleDB;
    }

}

