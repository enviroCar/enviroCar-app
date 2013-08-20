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

import java.util.ArrayList;

import org.envirocar.app.exception.TracksException;
import org.envirocar.app.logging.Logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Implementation of DbAdapter
 * 
 * @author jakob
 * 
 */

public class DbAdapterRemote implements DbAdapter {
	
	private static final Logger logger  = Logger.getLogger(DbAdapterRemote.class);

	// Database tables

	public static final String KEY_TIME = "measurement_time";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_RPM = "rpm";
	public static final String KEY_INTAKE_TEMPERATURE = "intake_temperature";
	public static final String KEY_INTAKE_PRESSURE = "intake_pressure";
	public static final String KEY_SPEED = "speed";
	public static final String KEY_MAF = "maf";
	public static final String KEY_CALCULATED_MAF = "calculated_maf";
	public static final String KEY_TRACK = "track";

	public static final String KEY_TRACK_ID = "_id";
	public static final String KEY_TRACK_NAME = "name";
	public static final String KEY_TRACK_DESCRIPTION = "descr";
	public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
	public static final String KEY_TRACK_CAR_MODEL = "car_model";
	public static final String KEY_TRACK_FUEL_TYPE = "fuel_type";
	public static final String KEY_TRACK_VIN = "vin";
	public static final String KEY_TRACK_SENSOR_ID = "sensorid";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Database parameters

	private static final String DATABASE_NAME = "obd2_remote";
	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_TABLE = "measurements";
	private static final String DATABASE_TABLE_TRACKS = "tracks";
	private static final String DATABASE_CREATE = "create table measurements "
			+ "(_id INTEGER primary key autoincrement, "
			+ "latitude BLOB, "
			+ "longitude BLOB, measurement_time BLOB, speed BLOB, rpm BLOB, intake_temperature BLOB, intake_pressure BLOB, maf BLOB, calculated_maf BLOB, track TEXT);";
	private static final String DATABASE_CREATE_TRACK = "create table tracks"
			+ " (_id TEXT primary key, " + "name BLOB, " + "descr BLOB, "
			+ "car_manufacturer BLOB, " + "car_model BLOB, "
			+ "fuel_type BLOB, " 
			+ "vin BLOB, "
			+ "sensorid BLOB);";

	private final Context mCtx;

	/**
	 * Database helper class
	 * 
	 * @author jakob
	 * 
	 */

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
			logger.info("Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS measurements");
			db.execSQL("DROP TABLE IF EXISTS tracks");
			onCreate(db);
		}

	}

	public DbAdapterRemote(Context ctx) {
		this.mCtx = ctx;

	}

	@Override
	public DbAdapter open() {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	@Override
	public void close() {
		mDb.close();
		mDbHelper.close();
	}

	@Override
	public void insertMeasurement(Measurement measurement) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_LATITUDE, measurement.getLatitude());
		initialValues.put(KEY_LONGITUDE, measurement.getLongitude());
		initialValues.put(KEY_TIME, measurement.getMeasurementTime());
		initialValues.put(KEY_SPEED, measurement.getSpeed());
		initialValues.put(KEY_RPM, measurement.getRpm());
		initialValues.put(KEY_INTAKE_TEMPERATURE, measurement.getIntakeTemperature());
		initialValues.put(KEY_INTAKE_PRESSURE, measurement.getIntakePressure());
		initialValues.put(KEY_MAF, measurement.getMaf());
		initialValues.put(KEY_CALCULATED_MAF, measurement.getCalculatedMaf());
		initialValues.put(KEY_TRACK,
				String.valueOf(measurement.getTrack().getId()));

		mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	private ArrayList<Measurement> getAllMeasurementsForTrack(Track track) {
		ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();

		Cursor c = mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME, KEY_SPEED, KEY_RPM, KEY_INTAKE_TEMPERATURE, KEY_INTAKE_PRESSURE, KEY_MAF, KEY_CALCULATED_MAF },
		// null,
				KEY_TRACK + "=\"" + track.getId() + "\"", null, null, null,
				// null
				KEY_TIME + " ASC");

		c.moveToFirst();

		for (int i = 0; i < c.getCount(); i++) {

			String row = c.getString(0);
			String lat = c.getString(1);
			String lon = c.getString(2);
			String time = c.getString(3);
			String speed = c.getString(4);
			String rpm = c.getString(5);
			String intake_temperature = c.getString(6);
			String intake_pressure = c.getString(7);
			String maf = c.getString(8);
			String calculated_maf = c.getString(9);
			// String track = c.getString(6);

			Measurement measurement = new Measurement(Float.valueOf(lat),
					Float.valueOf(lon));
			measurement.setId(Integer.valueOf(row));
			measurement.setMeasurementTime(Long.valueOf(time));
			measurement.setSpeed(Integer.valueOf(speed));
			measurement.setRpm(Integer.valueOf(rpm));
			measurement.setIntakeTemperature(Integer
					.valueOf(intake_temperature));
			measurement.setIntakePressure(Integer.valueOf(intake_pressure));
			measurement.setMaf(Double.valueOf(maf));
			measurement.setCalculatedMaf(Double.valueOf(calculated_maf));
			measurement.setTrack(track);

			allMeasurements.add(measurement);

			c.moveToNext();
		}

		c.close();
		return allMeasurements;
	}

	public boolean trackExistsInDatabase(String id) {
		Cursor count = mDb
				.rawQuery("SELECT COUNT(_id) FROM tracks WHERE _id =\"" + id
						+ "\"", null);
		count.moveToFirst();
		int ct = count.getInt(0);
		count.close();
		return (ct == 1 ? true : false);
	}

	@Override
	public Track getTrack(String id) {
		Track t = new Track(id);

		Cursor c = mDb.query(DATABASE_TABLE_TRACKS, new String[] { KEY_ROWID,
				KEY_TRACK_NAME, KEY_TRACK_DESCRIPTION,
				KEY_TRACK_CAR_MANUFACTURER, KEY_TRACK_CAR_MODEL,
				KEY_TRACK_FUEL_TYPE, KEY_TRACK_VIN, KEY_TRACK_SENSOR_ID }, KEY_ROWID + " = \"" + id
				+ "\"", null, null, null, null);

		c.moveToFirst();

		t.setId(c.getString(0));
		t.setName(c.getString(1));
		t.setDescription(c.getString(2));
		t.setCarManufacturer(c.getString(3));
		t.setCarModel(c.getString(4));
		t.setFuelType(c.getString(5));
		t.setVin(c.getString(6));
		t.setSensorID(c.getString(7));

		c.close();

		t.setMeasurementsAsArrayList(getAllMeasurementsForTrack(t));
		t.setLocalTrack(false);
		return t;
	}
	
	@Override
	public boolean hasTrack(String id) {
		Cursor c = mDb.query(DATABASE_TABLE_TRACKS, new String[] { KEY_ROWID,
				KEY_TRACK_NAME, KEY_TRACK_DESCRIPTION,
				KEY_TRACK_CAR_MANUFACTURER, KEY_TRACK_CAR_MODEL,
				KEY_TRACK_FUEL_TYPE, KEY_TRACK_VIN, KEY_TRACK_SENSOR_ID }, KEY_ROWID + " = \"" + id
				+ "\"", null, null, null, null);
		if (c.getCount() > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void deleteAllTracks() {
		mDb.delete(DATABASE_TABLE, null, null);
		mDb.delete(DATABASE_TABLE_TRACKS, null, null);
	}

	@Override
	public int getNumberOfStoredTracks() {
		Cursor count = mDb.rawQuery("SELECT COUNT(_id) FROM tracks", null);
		count.moveToFirst();
		int ct = count.getInt(0);
		count.close();
		return ct;
	}

	@Override
	public boolean isOpen() {
		return mDb.isOpen();
	}

	@Override
	public long insertTrack(Track track) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_TRACK_ID, track.getId());
		initialValues.put(KEY_TRACK_NAME, track.getName());
		initialValues.put(KEY_TRACK_DESCRIPTION, track.getDescription());
		initialValues.put(KEY_TRACK_CAR_MANUFACTURER,
				track.getCarManufacturer());
		initialValues.put(KEY_TRACK_CAR_MODEL, track.getCarModel());
		initialValues.put(KEY_TRACK_FUEL_TYPE, track.getFuelType());
		initialValues.put(KEY_TRACK_VIN, track.getVin());
		initialValues.put(KEY_TRACK_SENSOR_ID, track.getSensorID());

		return mDb.insert(DATABASE_TABLE_TRACKS, null, initialValues);
	}

	public long insertTrackWithMeasurements(Track track) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_TRACK_ID, track.getId());
		initialValues.put(KEY_TRACK_NAME, track.getName());
		initialValues.put(KEY_TRACK_DESCRIPTION, track.getDescription());
		initialValues.put(KEY_TRACK_CAR_MANUFACTURER,
				track.getCarManufacturer());
		initialValues.put(KEY_TRACK_CAR_MODEL, track.getCarModel());
		initialValues.put(KEY_TRACK_FUEL_TYPE, track.getFuelType());
		initialValues.put(KEY_TRACK_VIN, track.getVin());
		initialValues.put(KEY_TRACK_SENSOR_ID, track.getSensorID());

		for (Measurement m : track.getMeasurements()) {
			insertMeasurement(m);
		}

		return mDb.insert(DATABASE_TABLE_TRACKS, null, initialValues);
	}

	@Override
	public ArrayList<Track> getAllTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>();

		Cursor c = mDb.query(DATABASE_TABLE_TRACKS, null, null, null, null,
				null, null);
		// Cursor c = mDb.rawQuery("SELECT * from \"tracks\"", null);

		c.moveToFirst();

		for (int i = 0; i < c.getCount(); i++) {
			tracks.add(getTrack(c.getString(0)));
			c.moveToNext();
		}
		c.close();

		return tracks;
	}

	@Override
	public boolean updateTrack(Track track) {

		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_TRACK_ID, track.getId());
		initialValues.put(KEY_TRACK_NAME, track.getName());
		initialValues.put(KEY_TRACK_DESCRIPTION, track.getDescription());
		initialValues.put(KEY_TRACK_CAR_MANUFACTURER,
				track.getCarManufacturer());
		initialValues.put(KEY_TRACK_CAR_MODEL, track.getCarModel());
		initialValues.put(KEY_TRACK_FUEL_TYPE, track.getFuelType());
		initialValues.put(KEY_TRACK_VIN, track.getVin());
		initialValues.put(KEY_TRACK_SENSOR_ID, track.getSensorID());

		long result = mDb.replace(DATABASE_TABLE_TRACKS, null, initialValues);
		return (result != -1 ? true : false);
	}

	@Override
	public Track getLastUsedTrack() throws TracksException {
		throw new TracksException(
				"This is not applicable for the remote adapter.");
	}

	@Override
	public void deleteTrack(String id) {
		mDb.delete(DATABASE_TABLE, KEY_TRACK + "='" + id + "'", null);
		mDb.delete(DATABASE_TABLE_TRACKS, "_id='" + id + "'", null);
	}

}
