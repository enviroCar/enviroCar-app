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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.Track.TrackStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

	private static DbAdapterImpl instance;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private final Context mCtx;
	
	private DbAdapterImpl(Context ctx) {
		this.mCtx = ctx;
	}
	
	public static void init(Context ctx) throws InstantiationException {
		instance = new DbAdapterImpl(ctx);
		instance.openConnection();
		logger.info("init DbAdapterImpl; Hash: "+System.identityHashCode(instance));
	}
	
	public static DbAdapter instance() {
		logger.info("Returning DbAdapterImpl; Hash: "+System.identityHashCode(instance));
		return instance;
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
			logger.info("Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS measurements");
			db.execSQL("DROP TABLE IF EXISTS tracks");
			onCreate(db);
		}

	}
	
	@Override
	public DbAdapter open() {
		// deprecated
		return this;
	}
	
	private void openConnection() throws InstantiationException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		
		if (mDb == null) throw new InstantiationException("Database object is null");
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
	public synchronized void insertMeasurement(Measurement measurement) throws MeasurementsException {
		if (measurement.getTrack() == null) {
			throw new MeasurementsException("No Track is linked to this measurement.");
		}
		
		ContentValues values = new ContentValues();
		
		values.put(KEY_MEASUREMENT_LATITUDE, measurement.getLatitude());
		values.put(KEY_MEASUREMENT_LONGITUDE, measurement.getLongitude());
		values.put(KEY_MEASUREMENT_TIME, measurement.getTime());
		values.put(KEY_MEASUREMENT_TRACK, measurement.getTrack().getId());
		String propertiesString;
		try {
			propertiesString = createJsonObjectForProperties(measurement).toString();
		} catch (JSONException e) {
			logger.warn(e.getMessage(), e);
			throw new MeasurementsException(e.getMessage());
		}
		values.put(KEY_MEASUREMENT_PROPERTIES, propertiesString);
		
		mDb.insert(TABLE_MEASUREMENT, null, values);
	}

	@Override
	public synchronized void insertNewMeasurement(Measurement measurement) throws MeasurementsException, TrackAlreadyFinishedException {
		if (measurement.getTrack() == null) {
			throw new MeasurementsException("No Track is linked to this measurement.");
		}
		else if (measurement.getTrack().isFinished()) {
			throw new TrackAlreadyFinishedException("The linked track ("+measurement.getTrack().getId()+") is already finished!");
		}

		insertMeasurement(measurement);
	}
	
	@Override
	public synchronized long insertTrack(Track track) {
		logger.debug("insertTrack: "+track.getId());
		ContentValues values = createDbEntry(track);

		long result = mDb.insert(TABLE_TRACK, null, values);
		
		removeMeasurementArtifacts(result);
		
		return result;
	}

	private void removeMeasurementArtifacts(long id) {
		mDb.delete(TABLE_MEASUREMENT, KEY_MEASUREMENT_TRACK + "='" + id + "'", null);		
	}

	@Override
	public synchronized boolean updateTrack(Track track) {
		logger.debug("updateTrack: "+track.getId());
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
		Cursor c = mDb.query(TABLE_TRACK, new String[] {KEY_TRACK_ID}, null, null, null, null, null);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			long id = c.getLong(c.getColumnIndex(KEY_TRACK_ID));
			try {
				tracks.add(getTrack(id, lazyMeasurements));
			} catch (TrackWithoutMeasurementsException e) {
				logger.warn("Could not find any measurements for the track in the database. Removing.");
				deleteTrack(id);
			}
			c.moveToNext();
		}
		c.close();
		return tracks;
	}
	
	@Override
	public Track getTrack(long id, boolean lazyMeasurements) throws TrackWithoutMeasurementsException {
		Cursor c = getCursorForTrackID(id);
		if (!c.moveToFirst()) {
			return null;
		}

		Track track = Track.createTrackWithId(c.getLong(c.getColumnIndex(KEY_TRACK_ID)), this);
		track.setName(c.getString(c.getColumnIndex(KEY_TRACK_NAME)));
		track.setDescription(c.getString(c.getColumnIndex(KEY_TRACK_DESCRIPTION)));
		track.setRemoteID(c.getString(c.getColumnIndex(KEY_TRACK_REMOTE)));
		
		if (track.isRemoteTrack()) {
			/*
			 * remote tracks are always finished
			 */
			track.setStatus(TrackStatus.FINISHED);
		}
		
		int statusColumn = c.getColumnIndex(KEY_TRACK_STATE);
		if (statusColumn != -1) {
			track.setStatus(TrackStatus.valueOf(c.getString(statusColumn)));
		}
		else {
			/*
			 * if its a legacy track (column not there), set to finished
			 */
			track.setStatus(TrackStatus.FINISHED);
		}
		
		try {
			track.setMetadata(TrackMetadata.fromJson(c.getString(c.getColumnIndex(KEY_TRACK_METADATA))));
		} catch (JSONException e) {
			logger.warn(e.getMessage());
		}
		
		track.setCar(createCarFromCursor(c));

		c.close();

		if (!lazyMeasurements) {
			List<Measurement> measurements = getAllMeasurementsForTrack(track);
			track.setMeasurementsAsArrayList(measurements);	
		} else {
			track.setLazyLoadingMeasurements(true);
			Measurement first = getFirstMeasurementForTrack(track);
			Measurement last = getLastMeasurementForTrack(track);
			track.setStartTime(first.getTime());
			track.setEndTime(last.getTime());
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
		FuelType fuelType = FuelType.valueOf(c.getString(c.getColumnIndex(KEY_TRACK_CAR_FUEL_TYPE)));
		int engineDisplacement = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT));
		int year = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_YEAR));
		return new Car(fuelType, manufacturer, model, carId, year, engineDisplacement);
	}

	private Measurement getLastMeasurementForTrack(Track track) throws TrackWithoutMeasurementsException {
		Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
				KEY_MEASUREMENT_TRACK + "=\"" + track.getId() + "\"", null, null, null, KEY_MEASUREMENT_TIME + " DESC", "1");
	
		if (!c.moveToFirst()) {
			deleteTrackAndThrowException(track);
		}
	
		Measurement measurement = buildMeasurementFromCursor(track, c);
	
		return measurement;
	}

	private Measurement getFirstMeasurementForTrack(Track track) throws TrackWithoutMeasurementsException {
		Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
				KEY_MEASUREMENT_TRACK + "=\"" + track.getId() + "\"", null, null, null, KEY_MEASUREMENT_TIME + " ASC", "1");
	
		if (!c.moveToFirst()) {
			deleteTrackAndThrowException(track);
		}
	
		Measurement measurement = buildMeasurementFromCursor(track, c);
	
		return measurement;
	}

	private void deleteTrackAndThrowException(Track track) throws TrackWithoutMeasurementsException {
		deleteTrack(track.getId());
		throw new TrackWithoutMeasurementsException(track);		
	}

	private Measurement buildMeasurementFromCursor(Track track, Cursor c) {
		double lat = c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LATITUDE));
		double lon = c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LONGITUDE));
		long time = c.getLong(c.getColumnIndex(KEY_MEASUREMENT_TIME));
		String rawData = c.getString(c.getColumnIndex(KEY_MEASUREMENT_PROPERTIES));
		Measurement measurement = new Measurement(lat, lon);
		measurement.setTime(time);
		measurement.setTrack(track);
		
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
	public Track getTrack(long id) throws TrackWithoutMeasurementsException {
		return getTrack(id, false);
	}

	@Override
	public boolean hasTrack(long id) {
		Cursor cursor = getCursorForTrackID(id);
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
		Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_ID + ") FROM " + TABLE_TRACK, null);
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
	public void deleteTrack(long id) {
		logger.debug("deleteTrack: "+id);
		mDb.delete(TABLE_TRACK, KEY_TRACK_ID + "='" + id + "'", null);
		removeMeasurementArtifacts(id);
	}
	
	@Override
	public int getNumberOfRemoteTracks() {
		Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_REMOTE + ") FROM " + TABLE_TRACK, null);		
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
		Cursor cursor = mDb.rawQuery("SELECT COUNT(" + KEY_TRACK_REMOTE + ") FROM " + TABLE_TRACK, null);		
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();
		logger.info("" + count);
		mDb.delete(TABLE_TRACK, KEY_TRACK_REMOTE + " IS NOT NULL", null);
	}

	@Override
	public List<Track> getAllLocalTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>();
		Cursor c = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_REMOTE + " IS NULL", null, null, null, null);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			try {
				tracks.add(getTrack(c.getLong(c.getColumnIndex(KEY_TRACK_ID))));
			} catch (TrackWithoutMeasurementsException e) {
				logger.warn(e.getMessage());
			}
			c.moveToNext();
		}
		c.close();
		return tracks;
	}

	private ContentValues createDbEntry(Track track) {
		ContentValues values = new ContentValues();
		if (track.getId() != 0) {
			values.put(KEY_TRACK_ID, track.getId());
		}
		values.put(KEY_TRACK_NAME, track.getName());
		values.put(KEY_TRACK_DESCRIPTION, track.getDescription());
		values.put(KEY_TRACK_REMOTE, track.getRemoteID());
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
		Cursor cursor = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_ID + " = \"" + id + "\"", null, null, null, null);
		return cursor;
	}

	@Override
	public List<Measurement> getAllMeasurementsForTrack(Track track) throws TrackWithoutMeasurementsException {
		ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();
	
		Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
				KEY_MEASUREMENT_TRACK + "=\"" + track.getId() + "\"", null, null, null, KEY_MEASUREMENT_TIME + " ASC");
	
		if (!c.moveToFirst()) {
			deleteTrackAndThrowException(track);
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
		Car car = CarManager.instance().getCar();
		Track track = Track.createNewLocalTrack(this);
		track.setCar(car);
		track.setName("Track " + date);
		track.setDescription(String.format(mCtx.getString(R.string.default_track_description), car != null ? car.getModel() : "null"));
		updateTrack(track);
		logger.info("createNewTrack: "+ track.getName());
		return track;
	}

	@Override
	public Track finishCurrentTrack() {
		Track last = getLastUsedTrack();
		if (last != null) {
			if (last.getLastMeasurement() == null) {
				deleteTrack(last.getId());
			}
			last.setStatus(TrackStatus.FINISHED);
			updateTrack(last);
		}
		return last;
	}

	@Override
	public void updateCarIdOfTracks(String currentId, String newId) {
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_TRACK_CAR_ID, newId);
		
		mDb.update(TABLE_TRACK, newValues, KEY_TRACK_CAR_ID + "=?", new String[] {currentId});
	}

}
