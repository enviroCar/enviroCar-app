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

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.envirocar.core.logging.Logger;

/**
 * @author dewall
 */
class EnviroCarDBCallback extends SupportSQLiteOpenHelper.Callback {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBOpenHelper.class);

    /**
     * Creates a new Callback to get database lifecycle events.
     *
     * @param version The version for the database instance. See {@link #version}.
     */
    public EnviroCarDBCallback(int version) {
        super(version);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
        LOG.info("On create enviroCar database");
//            db.execSQL("PRAGMA foreign_keys=ON;");
        db.execSQL(TrackTable.CREATE);
        db.execSQL(MeasurementTable.CREATE);
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.info("On update enviroCar database");
        db.execSQL(MeasurementTable.DELETE);
        db.execSQL(TrackTable.DELETE);
        db.execSQL(TrackTable.CREATE);
        db.execSQL(MeasurementTable.CREATE);

    }
}
