/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.model.Position;
import org.envirocar.app.model.TrackId;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    protected CarManager mCarManager;

    private TrackId activeTrackReference;

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
    public synchronized void insertMeasurement(Measurement measurement) throws
            TrackAlreadyFinishedException, MeasurementSerializationException {
        insertMeasurement(measurement, false);
    }

    @Override
    public synchronized void insertMeasurement(Measurement measurement, boolean ignoreFinished)
            throws TrackAlreadyFinishedException, MeasurementSerializationException {
        logger.warn("insertMeasurement()");
        if (!ignoreFinished) {
            Track tempTrack = getTrack(measurement.getTrackId(), true);
            if (tempTrack.isFinished()) {
                throw new TrackAlreadyFinishedException("The linked track (" + tempTrack
                        .getTrackId() + ") is already finished!");
            }
        }

        logger.warn("Inserting measurements: " + measurement);

        ContentValues values = new ContentValues();

        values.put(KEY_MEASUREMENT_LATITUDE, measurement.getLatitude());
        values.put(KEY_MEASUREMENT_LONGITUDE, measurement.getLongitude());
        values.put(KEY_MEASUREMENT_TIME, measurement.getTime());
        values.put(KEY_MEASUREMENT_TRACK, measurement.getTrackId().getId());
        String propertiesString;
        try {
            propertiesString = createJsonObjectForProperties(measurement).toString();
        } catch (JSONException e) {
            logger.warn(e.getMessage(), e);
            throw new MeasurementSerializationException(e);
        }
        values.put(KEY_MEASUREMENT_PROPERTIES, propertiesString);

        long res = mDb.insert(TABLE_MEASUREMENT, null, values);
        logger.warn("Measurement inserted in track = " + measurement.getTrackId() + ", " +
                "measurementID = " + res);
    }

    @Override
    public synchronized void insertNewMeasurement(Measurement measurement) throws
            TrackAlreadyFinishedException, MeasurementSerializationException {
        TrackId activeTrack = getActiveTrackReference(
                new Position(measurement.getLatitude(), measurement.getLongitude()));
        logger.warn("insertNewMewasurement(): activeTrack ID = " +activeTrack.getId());

        measurement.setTrackId(activeTrack);
        insertMeasurement(measurement);

        lastMeasurementsInsertionTimestamp = System.currentTimeMillis();
    }

    @Override
    public synchronized long insertTrack(Track track, boolean remote) {
        logger.warn("insertTrack(" + track.getTrackId() + "," + remote + ")");
        ContentValues values = createDbEntry(track);

        long result = mDb.insert(TABLE_TRACK, null, values);
        track.setTrackId(new TrackId(result));

        removeMeasurementArtifacts(result);

        for (Measurement m : track.getMeasurements()) {
            m.setTrackId(track.getTrackId());
            try {
                insertMeasurement(m, remote ? true : false);
            } catch (TrackAlreadyFinishedException e) {
                logger.warn(e.getMessage(), e);
            } catch (MeasurementSerializationException e) {
                logger.warn(e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    public synchronized long insertTrack(Track track) {
        return insertTrack(track, false);
    }

    private void removeMeasurementArtifacts(long id) {
        mDb.delete(TABLE_MEASUREMENT, KEY_MEASUREMENT_TRACK + "='" + id + "'", null);
    }

    @Override
    public synchronized boolean updateTrack(Track track) {
        logger.debug("updateTrack: " + track.getTrackId());
        ContentValues values = createDbEntry(track);
        long result = mDb.replace(TABLE_TRACK, null, values);
        return (result != -1 ? true : false);
    }

    @Override
    public ArrayList<Track> getAllTracks() {
        return getAllTracks(false);
    }

    @Override
    public ArrayList<Track> getAllTracks(boolean lazyMeasurements) {
        ArrayList<Track> tracks = new ArrayList<Track>();
        Cursor c = mDb.query(TABLE_TRACK, new String[]{KEY_TRACK_ID}, null, null, null, null, null);
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            long id = c.getLong(c.getColumnIndex(KEY_TRACK_ID));
            tracks.add(getTrack(new TrackId(id), lazyMeasurements));
            c.moveToNext();
        }
        c.close();
        return tracks;
    }

    @Override
    public Track getTrack(TrackId id, boolean lazyMeasurements) {
        logger.warn(String.format("getTrack(%s,%s)", "" + id, "" + lazyMeasurements));
        Cursor c = getCursorForTrackID(id.getId());
        if (!c.moveToFirst()) {
            return null;
        }

        String remoteId = c.getString(c.getColumnIndex(KEY_TRACK_REMOTE));

        Track track;
        if (remoteId != null && !remoteId.isEmpty()) {
            track = Track.createRemoteTrack(remoteId);
        } else {
            track = Track.createLocalTrack();
        }

        track.setTrackId(id);

        track.setName(c.getString(c.getColumnIndex(KEY_TRACK_NAME)));
        track.setDescription(c.getString(c.getColumnIndex(KEY_TRACK_DESCRIPTION)));

        int statusColumn = c.getColumnIndex(KEY_TRACK_STATE);
        if (statusColumn != -1) {
            track.setStatus(TrackStatus.valueOf(c.getString(statusColumn)));
        } else {
            /*
             * if its a legacy track (column not there), set to finished
			 */
            track.setStatus(TrackStatus.FINISHED);
        }

        String metadata = c.getString(c.getColumnIndex(KEY_TRACK_METADATA));
        if (metadata != null) {
            try {
                track.setMetadata(TrackMetadata.fromJson(c.getString(c.getColumnIndex
                        (KEY_TRACK_METADATA))));
            } catch (JSONException e) {
                logger.warn(e.getMessage(), e);
            }
        }

        track.setCar(createCarFromCursor(c));

        c.close();

        if (!lazyMeasurements) {
            loadMeasurements(track);
        } else {
            track.setLazyLoadingMeasurements(true);
            Measurement first = getFirstMeasurementForTrack(track);
            Measurement last = getLastMeasurementForTrack(track);

            if (first != null && last != null) {
                track.setStartTime(first.getTime());
                track.setEndTime(last.getTime());
            }

        }

        if (track.isRemoteTrack()) {
            /*
			 * remote tracks are always finished
			 */
            track.setStatus(TrackStatus.FINISHED);
        }

        return track;
    }

    private Car createCarFromCursor(Cursor c) {
        if (c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_YEAR)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_FUEL_TYPE)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT)) == null) {
            return null;
        }

        String manufacturer = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER));
        String model = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL));
        String carId = c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID));
        FuelType fuelType = FuelType.valueOf(c.getString(c.getColumnIndex
                (KEY_TRACK_CAR_FUEL_TYPE)));
        int engineDisplacement = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT));
        int year = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_YEAR));
        return new Car(fuelType, manufacturer, model, carId, year, engineDisplacement);
    }

    private Measurement getLastMeasurementForTrack(Track track) {
        Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
                KEY_MEASUREMENT_TRACK + "=\"" + track.getTrackId() + "\"", null, null, null,
                KEY_MEASUREMENT_TIME + " DESC", "1");


        Measurement measurement = null;
        if (c.moveToFirst()) {
            measurement = buildMeasurementFromCursor(track, c);
        }

        return measurement;
    }


    private Measurement getFirstMeasurementForTrack(Track track) {
        Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
                KEY_MEASUREMENT_TRACK + "=\"" + track.getTrackId() + "\"", null, null, null,
                KEY_MEASUREMENT_TIME + " ASC", "1");

        Measurement measurement = null;
        if (c.moveToFirst()) {
            measurement = buildMeasurementFromCursor(track, c);
        }

        return measurement;
    }


    private Measurement buildMeasurementFromCursor(Track track, Cursor c) {
        double lat = c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LATITUDE));
        double lon = c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LONGITUDE));
        long time = c.getLong(c.getColumnIndex(KEY_MEASUREMENT_TIME));
        String rawData = c.getString(c.getColumnIndex(KEY_MEASUREMENT_PROPERTIES));
        Measurement measurement = new Measurement(lat, lon);
        measurement.setTime(time);
        measurement.setTrackId(track.getTrackId());

        if (rawData != null) {
            try {
                JSONObject json = new JSONObject(rawData);
                JSONArray names = json.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String key = names.getString(j);
                        measurement.setProperty(PropertyKey.valueOf(key), json.getDouble(key));
                    }
                }
            } catch (JSONException e) {
                logger.severe("could not load properties", e);
            }
        }
        return measurement;
    }

    @Override
    public Track getTrack(TrackId id) {
        return getTrack(id, false);
    }

    @Override
    public boolean hasTrack(TrackId id) {
        Cursor cursor = getCursorForTrackID(id.getId());
        if (cursor.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void deleteAllTracks() {
        mDb.delete(TABLE_MEASUREMENT, null, null);
        mDb.delete(TABLE_TRACK, null, null);
    }

    @Override
    public int getNumberOfStoredTracks() {
        Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_ID + ") FROM " + TABLE_TRACK,
                null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    @Override
    public Track getLastUsedTrack(boolean lazyMeasurements) {
        ArrayList<Track> trackList = getAllTracks(lazyMeasurements);
        if (trackList.size() > 0) {
            Track track = trackList.get(trackList.size() - 1);
            return track;
        }

        return null;
    }

    @Override
    public Track getLastUsedTrack() {
        return getLastUsedTrack(false);
    }

    @Override
    public void deleteTrack(TrackId id) {
        logger.debug("deleteTrack: " + id);
        mDb.delete(TABLE_TRACK, KEY_TRACK_ID + "='" + id + "'", null);
        removeMeasurementArtifacts(id.getId());
    }

    @Override
    public int getNumberOfRemoteTracks() {
        Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_REMOTE + ") FROM " +
                TABLE_TRACK, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    @Override
    public int getNumberOfLocalTracks() {
        // TODO Auto-generated method stub
        logger.warn("implement it!!!");
        return 0;
    }

    @Override
    public void deleteAllLocalTracks() {
        // TODO Auto-generated method stub
        logger.warn("implement it!!!");
    }

    @Override
    public void deleteAllRemoteTracks() {
        Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_REMOTE + ") FROM " +
                TABLE_TRACK, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        logger.info("" + count);
        mDb.delete(TABLE_TRACK, KEY_TRACK_REMOTE + " IS NOT NULL", null);
    }

    @Override
    public List<Track> getAllLocalTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>();
        Cursor c = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_REMOTE + " IS NULL", null,
                null, null, null);
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            tracks.add(getTrack(new TrackId(c.getLong(c.getColumnIndex(KEY_TRACK_ID)))));
            c.moveToNext();
        }
        c.close();
        return tracks;
    }

    private ContentValues createDbEntry(Track track) {
        ContentValues values = new ContentValues();
        if (track.getTrackId() != null && track.getTrackId().getId() != 0) {
            values.put(KEY_TRACK_ID, track.getTrackId().getId());
        }
        values.put(KEY_TRACK_NAME, track.getName());
        values.put(KEY_TRACK_DESCRIPTION, track.getDescription());
        if (track.isRemoteTrack()) {
            values.put(KEY_TRACK_REMOTE, ((RemoteTrack) track).getRemoteID());
        }
        values.put(KEY_TRACK_STATE, track.getStatus().toString());
        if (track.getCar() != null) {
            values.put(KEY_TRACK_CAR_MANUFACTURER, track.getCar().getManufacturer());
            values.put(KEY_TRACK_CAR_MODEL, track.getCar().getModel());
            values.put(KEY_TRACK_CAR_FUEL_TYPE, track.getCar().getFuelType().name());
            values.put(KEY_TRACK_CAR_ID, track.getCar().getId());
            values.put(KEY_TRACK_CAR_ENGINE_DISPLACEMENT, track.getCar().getEngineDisplacement());
            values.put(KEY_TRACK_CAR_YEAR, track.getCar().getConstructionYear());
        }

        if (track.getMetadata() != null) {
            try {
                values.put(KEY_TRACK_METADATA, track.getMetadata().toJsonString());
            } catch (JSONException e) {
                logger.warn(e.getMessage(), e);
            }
        }

        return values;
    }

    public JSONObject createJsonObjectForProperties(Measurement measurement) throws JSONException {
        JSONObject result = new JSONObject();

        Map<PropertyKey, Double> properties = measurement.getAllProperties();
        for (PropertyKey key : properties.keySet()) {
            result.put(key.name(), properties.get(key));
        }

        return result;
    }

    private Cursor getCursorForTrackID(long id) {
        Cursor cursor = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_ID + " = \"" + id +
                "\"", null, null, null, null);
        return cursor;
    }

    @Override
    public List<Measurement> getAllMeasurementsForTrack(Track track) {
        logger.warn("getAllMeasurementsForTrack id=" + track.getTrackId());
        ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();

        Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
                KEY_MEASUREMENT_TRACK + "=\"" + track.getTrackId() + "\"", null, null, null,
                KEY_MEASUREMENT_TIME + " ASC");

        if (!c.moveToFirst()) {
            return Collections.emptyList();
        }

        for (int i = 0; i < c.getCount(); i++) {

            Measurement measurement = buildMeasurementFromCursor(track, c);
            allMeasurements.add(measurement);
            c.moveToNext();
        }

        c.close();

        return allMeasurements;
    }

    @Override
    public Track createNewTrack() {
        finishCurrentTrack();

        String date = format.format(new Date());
        Car car = mCarManager.getCar();
        Track track = Track.createLocalTrack();
        track.setCar(car);
        track.setName("Track " + date);
        track.setDescription(String.format(mContext.getString(R.string.default_track_description)
                , car != null ? car.getModel() : "null"));
        insertTrack(track);
        return track;
    }

    @Override
    public synchronized Track finishCurrentTrack() {
        Track last = getLastUsedTrack();
        if (last != null) {
            if (last.getLastMeasurement() == null) {
                deleteTrack(last.getTrackId());
            }
            last.setStatus(TrackStatus.FINISHED);
            updateTrack(last);

            if (last.getTrackId().equals(activeTrackReference)) {
                logger.info("removing activeTrackReference: " + activeTrackReference);
            } else {
                logger.info(String.format(
                        "Finished track did not have the same ID as the activeTrackReference. " +
                                "Finished: %s vs. active: %s",
                        last.getTrackId(), activeTrackReference));
            }

            activeTrackReference = null;
        }
        return last;
    }

    @Override
    public void updateCarIdOfTracks(String currentId, String newId) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_TRACK_CAR_ID, newId);

        mDb.update(TABLE_TRACK, newValues, KEY_TRACK_CAR_ID + "=?", new String[]{currentId});
    }

    @Override
    public synchronized TrackId getActiveTrackReference(Position pos) {
		/*
		 * make this performant. if we have an activeTrackReference
		 * and its not too old, use it
		 */
        if (activeTrackReference != null &&
                System.currentTimeMillis() - lastMeasurementsInsertionTimestamp < this
                        .maxTimeBetweenMeasurements / 10) {
            logger.info("returning activeTrackReference: " + activeTrackReference);
            return activeTrackReference;
        }

        Track lastUsed = getLastUsedTrack(true);

        if (!trackIsStillActive(lastUsed, pos)) {
            lastUsed = createNewTrack();
        }

        logger.info(
                String.format("getActiveTrackReference - Track: %s / id: %s",
                        lastUsed.getName(),
                        lastUsed.getTrackId()));

        activeTrackReference = lastUsed.getTrackId();

        if (this.obdDeviceMetadata != null) {
            updateTrackMetadata(activeTrackReference, this.obdDeviceMetadata);
        }

        return activeTrackReference;
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

        if (lastUsedTrack != null && lastUsedTrack.getStatus() != TrackStatus.FINISHED &&
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
            logger.info(String.format("Should create new Track. Last was null? %b; Last status " +
                            "was: %s; Last measurement: %s",
                    lastUsedTrack == null,
                    lastUsedTrack == null ? "n/a" : lastUsedTrack.getStatus().toString(),
                    lastUsedTrack == null ? "n/a" : lastUsedTrack.getLastMeasurement()));

            if (lastUsedTrack != null && !lastUsedTrack.isRemoteTrack()) {
                List<Measurement> measurements = lastUsedTrack.getMeasurements();
                if (measurements == null || measurements.isEmpty()) {
                    logger.info(String.format("Track %s did not contain measurements and will not" +
                            " be used. Deleting!", lastUsedTrack.getTrackId()));
//                    deleteTrack(lastUsedTrack.getTrackId());
                }
            }

            return false;
        }

    }

    @Override
    public void updateTrackMetadata(TrackId trackId, TrackMetadata trackMetadata) {
        Track tempTrack = getTrack(trackId, true);
        tempTrack.updateMetadata(trackMetadata);
        updateTrack(tempTrack);
    }

    @Override
    public void transitLocalToRemoteTrack(Track track, String remoteId) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_TRACK_REMOTE, remoteId);

        mDb.update(TABLE_TRACK, newValues, KEY_TRACK_ID + "=?", new String[]{
                Long.toString(track.getTrackId().getId())
        });
    }

    @Override
    public void loadMeasurements(Track track) {
        List<Measurement> measurements = getAllMeasurementsForTrack(track);
        track.setMeasurementsAsArrayList(measurements);
        track.setLazyLoadingMeasurements(false);
    }

    @Override
    public void setConnectedOBDDevice(TrackMetadata obdDeviceMetadata) {
        this.obdDeviceMetadata = obdDeviceMetadata;

        if (this.activeTrackReference != null) {
            updateTrackMetadata(activeTrackReference, obdDeviceMetadata);
        }
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
