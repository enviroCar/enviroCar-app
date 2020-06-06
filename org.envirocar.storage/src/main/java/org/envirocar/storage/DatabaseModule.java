/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.*;

import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.SqlBrite;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

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
        return Room.databaseBuilder(context,EnviroCarVehicleDB.class,VECHILE_DATABASE_NAME)
                .addCallback(new EnviroCarVehicleDBCallback())
                .build();
    }

}

