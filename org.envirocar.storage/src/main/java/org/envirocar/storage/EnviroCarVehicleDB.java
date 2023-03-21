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
package org.envirocar.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.migration.Migration;
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

@Database(entities = {Manufacturers.class, Vehicles.class, PowerSource.class}, version = 2)
public abstract class EnviroCarVehicleDB extends RoomDatabase {

    //DAO car selection
    public abstract LocalManufacturersDAO manufacturersDAO();

    public abstract LocalPowerSourcesDAO powerSourcesDAO();

    public abstract LocalVehicleDAO vehicleDAO();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE vehicles "
            + " ADD COLUMN emission_class TEXT");      
        }
      };
      
}
