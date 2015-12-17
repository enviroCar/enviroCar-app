/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.core.delete.Position;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.inject.Inject;

public class DbAdapterImpl implements DbAdapter {

    private static final Logger logger = Logger.getLogger(DbAdapterImpl.class);

    public static final String TABLE_MEASUREMENT = "measurements";
    public static final String KEY_MEASUREMENT_TIME = "time";
    public static final String KEY_MEASUREMENT_LONGITUDE = "longitude";
    public static final String KEY_MEASUREMENT_LATITUDE = "latitude";
    public static final String KEY_MEASUREMENT_ROWID = "_id";
    public static final String KEY_MEASUREMENT_PROPERTIES = "properties";
    public static final String KEY_MEASUREMENT_TRACK = "track";
    public static final String[] ALL_MEASUREMENT_KEYS = new String[]{
            KEY_MEASUREMENT_ROWID,
            KEY_MEASUREMENT_TIME,
            KEY_MEASUREMENT_LONGITUDE,
            KEY_MEASUREMENT_LATITUDE,
            KEY_MEASUREMENT_PROPERTIES,
            KEY_MEASUREMENT_TRACK
    };

    public static final String TABLE_TRACK = "tracks";
    public static final String KEY_TRACK_ID = "_id";
    public static final String KEY_TRACK_NAME = "name";
    public static final String KEY_TRACK_DESCRIPTION = "descr";
    public static final String KEY_TRACK_REMOTE = "remoteId";
    public static final String KEY_TRACK_STATE = "state";
    public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
    public static final String KEY_TRACK_CAR_MODEL = "car_model";
    public static final String KEY_TRACK_CAR_FUEL_TYPE = "fuel_type";
    public static final String KEY_TRACK_CAR_YEAR = "car_construction_year";
    public static final String KEY_TRACK_CAR_ENGINE_DISPLACEMENT = "engine_displacement";
    public static final String KEY_TRACK_CAR_VIN = "vin";
    public static final String KEY_TRACK_CAR_ID = "carId";
    public static final String KEY_TRACK_METADATA = "trackMetadata";

    public static final String[] ALL_TRACK_KEYS = new String[]{
            KEY_TRACK_ID,
            KEY_TRACK_NAME,
            KEY_TRACK_DESCRIPTION,
            KEY_TRACK_REMOTE,
            KEY_TRACK_STATE,
            KEY_TRACK_METADATA,
            KEY_TRACK_CAR_MANUFACTURER,
            KEY_TRACK_CAR_MODEL,
            KEY_TRACK_CAR_FUEL_TYPE,
            KEY_TRACK_CAR_ENGINE_DISPLACEMENT,
            KEY_TRACK_CAR_YEAR,
            KEY_TRACK_CAR_VIN,
            KEY_TRACK_CAR_ID
    };

    private static final String DATABASE_NAME = "obd2";
    private static final int DATABASE_VERSION = 9;

    private static final String DATABASE_CREATE = "create table " + TABLE_MEASUREMENT + " " +
            "(" + KEY_MEASUREMENT_ROWID + " INTEGER primary key autoincrement, " +
            KEY_MEASUREMENT_LATITUDE + " BLOB, " +
            KEY_MEASUREMENT_LONGITUDE + " BLOB, " +
            KEY_MEASUREMENT_TIME + " BLOB, " +
            KEY_MEASUREMENT_PROPERTIES + " BLOB, " +
            KEY_MEASUREMENT_TRACK + " INTEGER);";
    private static final String DATABASE_CREATE_TRACK = "create table " + TABLE_TRACK + " " +
            "(" + KEY_TRACK_ID + " INTEGER primary key, " +
            KEY_TRACK_NAME + " BLOB, " +
            KEY_TRACK_DESCRIPTION + " BLOB, " +
            KEY_TRACK_REMOTE + " BLOB, " +
            KEY_TRACK_STATE + " BLOB, " +
            KEY_TRACK_METADATA + " BLOB, " +
            KEY_TRACK_CAR_MANUFACTURER + " BLOB, " +
            KEY_TRACK_CAR_MODEL + " BLOB, " +
            KEY_TRACK_CAR_FUEL_TYPE + " BLOB, " +
            KEY_TRACK_CAR_ENGINE_DISPLACEMENT + " BLOB, " +
            KEY_TRACK_CAR_YEAR + " BLOB, " +
            KEY_TRACK_CAR_VIN + " BLOB, " +
            KEY_TRACK_CAR_ID + " BLOB);";

    private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

    private static final long DEFAULT_MAX_TIME_BETWEEN_MEASUREMENTS = 1000 * 60 * 15;

    private static final double DEFAULT_MAX_DISTANCE_BETWEEN_MEASUREMENTS = 3.0;


    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected CarPreferenceHandler mCarManager;

    private Track.TrackId activeTrackReference;

    private long lastMeasurementsInsertionTimestamp;

    private long maxTimeBetweenMeasurements;

    private double maxDistanceBetweenMeasurements;

    private TrackMetadata obdDeviceMetadata;

    public DbAdapterImpl(Context context) throws InstantiationException {
        // Inject all annotated fields.
        ((Injector) context).injectObjects(this);

        this.maxTimeBetweenMeasurements = DEFAULT_MAX_TIME_BETWEEN_MEASUREMENTS;
        this.maxDistanceBetweenMeasurements = DEFAULT_MAX_DISTANCE_BETWEEN_MEASUREMENTS;

        this.mDbHelper = new DatabaseHelper(mContext);
        this.mDb = mDbHelper.getWritableDatabase();

        if (mDb == null) throw new InstantiationException("Database object is null");
    }


    @Override
    public DbAdapter open() {
        // deprecated
        return this;
    }

    @Override
    public void close() {
        mDb.close();
        mDbHelper.close();
    }

    @Override
    public boolean isOpen() {
        return mDb.isOpen();
    }

    @Override
    public void updateCarIdOfTracks(String currentId, String newId) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_TRACK_CAR_ID, newId);

        mDb.update(TABLE_TRACK, newValues, KEY_TRACK_CAR_ID + "=?", new String[]{currentId});
    }

    /**
     * This method determines whether it is necessary to create a new track or
     * of the current/last used track should be reused
     */
    private boolean trackIsStillActive(Track lastUsedTrack, Position location) {

        logger.info("trackIsStillActive: last? " + (lastUsedTrack == null ? "null" :
                lastUsedTrack.toString()));

        // New track if last measurement is more than 60 minutes
        // ago

        try {
            if (lastUsedTrack != null &&
                    lastUsedTrack.getTrackStatus() != Track.TrackStatus.FINISHED &&
                    lastUsedTrack.getLastMeasurement() != null) {

                if ((System.currentTimeMillis() - lastUsedTrack
                        .getLastMeasurement().getTime()) > this.maxTimeBetweenMeasurements) {
                    logger.info(String.format("Should create a new track: last measurement is more " +
                                    "than %d mins ago",
                            (int) (this.maxTimeBetweenMeasurements / 1000 / 60)));
                    return false;
                }

                // new track if last position is significantly different
                // from the current position (more than 3 km)
                else if (location == null || Util.getDistance(lastUsedTrack.getLastMeasurement()
                                .getLatitude(), lastUsedTrack.getLastMeasurement().getLongitude(),
                        location.getLatitude(), location.getLongitude()) > this
                        .maxDistanceBetweenMeasurements) {
                    logger.info(String.format("Should create a new track: last measurement's position" +
                                    " is more than %f km away",
                            this.maxDistanceBetweenMeasurements));
                    return false;
                }

                // TODO: New track if user clicks on create new track button

                // TODO: new track if VIN changed

                else {
                    logger.info("Should append to the last track: last measurement is close enough in" +
                            " space/time");
                    return true;
                }

            } else {
                logger.info("should craete a new track?");
//                logger.info(String.format("Should create new Track. Last was null? %b; Last status " +
//                                "was: %s; Last measurement: %s",
//                        lastUsedTrack == null,
//                        lastUsedTrack == null ? "n/a" : lastUsedTrack.getTrackStatus().toString(),
//                        lastUsedTrack == null ? "n/a" : lastUsedTrack.getLastMeasurement()));

                if (lastUsedTrack != null && !lastUsedTrack.isRemoteTrack()) {
                    List<Measurement> measurements = lastUsedTrack.getMeasurements();
                    if (measurements == null || measurements.isEmpty()) {
                        logger.info(String.format("Track %s did not contain measurements and will not" +
                                " be used. Deleting!", lastUsedTrack.getTrackID()));
                        //                    deleteLocalTrack(lastUsedTrack.getTrackId());
                    }
                }

                return false;
            }
        } catch (NoMeasurementsException e) {
            logger.warn(e.getMessage(), e);
            return false;
        }
    }

    public void transitLocalToRemoteTrack(Track track, String remoteId) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_TRACK_REMOTE, remoteId);

        mDb.update(TABLE_TRACK, newValues, KEY_TRACK_ID + "=?", new String[]{
                Long.toString(track.getTrackID().getId())
        });
    }


    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
            db.execSQL(DATABASE_CREATE_TRACK);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            logger.info("Upgrading database from version " + oldVersion + " to " + newVersion +
                    ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS measurements");
            db.execSQL("DROP TABLE IF EXISTS tracks");
            onCreate(db);
        }
    }
}
