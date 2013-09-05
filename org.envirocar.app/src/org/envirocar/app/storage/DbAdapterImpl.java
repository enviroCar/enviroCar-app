package org.envirocar.app.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.envirocar.app.exception.TracksException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement.PropertyKey;
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
	public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
	public static final String KEY_TRACK_CAR_MODEL = "car_model";
	public static final String KEY_TRACK_CAR_FUEL_TYPE = "fuel_type";
	public static final String KEY_TRACK_CAR_ENGINE_DISPLACEMENT = "engine_displacement";
	public static final String KEY_TRACK_CAR_VIN = "vin";
	public static final String KEY_TRACK_CAR_ID = "carId"; 
	
	public static final String[] ALL_TRACK_KEYS = new String[]{
		KEY_TRACK_ID,
		KEY_TRACK_NAME,
		KEY_TRACK_DESCRIPTION,
		KEY_TRACK_REMOTE,
		KEY_TRACK_CAR_MANUFACTURER,
		KEY_TRACK_CAR_MODEL,
		KEY_TRACK_CAR_FUEL_TYPE,
		KEY_TRACK_CAR_ENGINE_DISPLACEMENT,
		KEY_TRACK_CAR_VIN,
		KEY_TRACK_CAR_ID
	};

	private static final String DATABASE_NAME = "obd2";
	private static final int DATABASE_VERSION = 6;
	
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
			KEY_TRACK_CAR_MANUFACTURER + " BLOB, " + 
			KEY_TRACK_CAR_MODEL + " BLOB, " +
			KEY_TRACK_CAR_FUEL_TYPE + " BLOB, " +
			KEY_TRACK_CAR_ENGINE_DISPLACEMENT + " BLOB, " +
			KEY_TRACK_CAR_VIN + " BLOB, " +
			KEY_TRACK_CAR_ID + " BLOB);";
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private final Context mCtx;
	
	public DbAdapterImpl(Context ctx) {
		this.mCtx = ctx;
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
	public boolean isOpen() {
		return mDb.isOpen();
	}

	@Override
	public void insertMeasurement(Measurement measurement) {
		ContentValues values = new ContentValues();
		
		values.put(KEY_MEASUREMENT_LATITUDE, measurement.getLatitude());
		values.put(KEY_MEASUREMENT_LONGITUDE, measurement.getLongitude());
		values.put(KEY_MEASUREMENT_TIME, measurement.getTime());
		values.put(KEY_MEASUREMENT_TRACK, measurement.getTrack().getId());
		String propertiesString = createJsonObjectForProperties(measurement).toString();
		values.put(KEY_MEASUREMENT_PROPERTIES, propertiesString);
		
		mDb.insert(TABLE_MEASUREMENT, null, values);
	}
	
	@Override
	public long insertTrack(Track track) {
		ContentValues values = createDbEntry(track);

		return mDb.insert(TABLE_TRACK, null, values);
	}

	@Override
	public boolean updateTrack(Track track) {
		ContentValues values = createDbEntry(track);
		long result = mDb.replace(TABLE_TRACK, null, values);
		return (result != -1 ? true : false);
	}

	@Override
	public ArrayList<Track> getAllTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>();
		Cursor c = mDb.query(TABLE_TRACK, null, null, null, null, null, null);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			long id = c.getLong(c.getColumnIndex(KEY_TRACK_ID));
			tracks.add(getTrack(id));
			c.moveToNext();
		}
		c.close();
		return tracks;
	}

	@Override
	public Track getTrack(long id) {
		Cursor c = getCursorForTrackID(id);
		c.moveToFirst();

		Track track = Track.createDbTrack(c.getLong(c.getColumnIndex(KEY_TRACK_ID)));
		track.setName(c.getString(c.getColumnIndex(KEY_TRACK_NAME)));
		track.setDescription(c.getString(c.getColumnIndex(KEY_TRACK_DESCRIPTION)));
		track.setVin(c.getString(c.getColumnIndex(KEY_TRACK_CAR_VIN)));
		track.setRemoteID(c.getString(c.getColumnIndex(KEY_TRACK_REMOTE)));
		
		String manufacturer = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER));
		String model = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL));
		String carId = c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID));
		FuelType fuelType = FuelType.valueOf(c.getString(c.getColumnIndex(KEY_TRACK_CAR_FUEL_TYPE)));
		double engineDisplacement = c.getDouble(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT));
		track.setCar(new Car(fuelType, manufacturer, model, carId, engineDisplacement));

		c.close();

		ArrayList<Measurement> measurements = getAllMeasurementsForTrack(track);
		track.setMeasurementsAsArrayList(measurements);
		return track;
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
	public Track getLastUsedTrack() throws TracksException {
		ArrayList<Track> trackList = getAllTracks();
		if (trackList.size() > 0) {
			return trackList.get(trackList.size() - 1);
		} else
			throw new TracksException("No tracks in local database!");
	}

	@Override
	public void deleteTrack(long id) {
		mDb.delete(TABLE_MEASUREMENT, KEY_MEASUREMENT_TRACK + "='" + id + "'", null);
		mDb.delete(TABLE_TRACK, KEY_TRACK_ID + "='" + id + "'", null);
	}
	
	@Override
	public int getNumberOfRemoteTracks() {
		// TODO Auto-generated method stub
		logger.warn("implement it!!!");
		return 0;
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
		// TODO Auto-generated method stub
		logger.warn("implement it!!!");
	}

	@Override
	public List<Track> getAllLocalTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>();
		Cursor c = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_REMOTE + " IS NULL", null, null, null, null);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			tracks.add(getTrack(c.getLong(c.getColumnIndex(KEY_TRACK_ID))));
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
		if (track.getCar() != null) {
			values.put(KEY_TRACK_CAR_MANUFACTURER, track.getCar().getManufacturer());
			values.put(KEY_TRACK_CAR_MODEL, track.getCar().getModel());
			values.put(KEY_TRACK_CAR_FUEL_TYPE, track.getCar().getFuelType().name());
			values.put(KEY_TRACK_CAR_VIN, track.getVin());
			values.put(KEY_TRACK_CAR_ID, track.getCar().getId());
			values.put(KEY_TRACK_CAR_ENGINE_DISPLACEMENT, track.getCar().getEngineDisplacement());
		}
		return values;
	}

	private JSONObject createJsonObjectForProperties(Measurement measurement) {
		HashMap<String, Double> map = new HashMap<String, Double>();
		Map<PropertyKey, Double> properties = measurement.getAllProperties();
		for (PropertyKey key : properties.keySet()) {
			map.put(key.name(), properties.get(key));
		}
		return new JSONObject(map);
	}

	private Cursor getCursorForTrackID(long id) {
		Cursor cursor = mDb.query(TABLE_TRACK, ALL_TRACK_KEYS, KEY_TRACK_ID + " = \"" + id + "\"", null, null, null, null);
		return cursor;
	}

	private ArrayList<Measurement> getAllMeasurementsForTrack(Track track) {
		ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();
	
		Cursor c = mDb.query(TABLE_MEASUREMENT, ALL_MEASUREMENT_KEYS,
				KEY_MEASUREMENT_TRACK + "=\"" + track.getId() + "\"", null, null, null, KEY_MEASUREMENT_TIME + " ASC");
	
		c.moveToFirst();
	
		for (int i = 0; i < c.getCount(); i++) {
	
			String lat = c.getString(c.getColumnIndex(KEY_MEASUREMENT_LATITUDE));
			String lon = c.getString(c.getColumnIndex(KEY_MEASUREMENT_LONGITUDE));
			String time = c.getString(c.getColumnIndex(KEY_MEASUREMENT_TIME));
			String rawData = c.getString(c.getColumnIndex(KEY_MEASUREMENT_PROPERTIES));
			Measurement measurement = new Measurement(Float.valueOf(lat), Float.valueOf(lon));
			measurement.setTime(Long.valueOf(time));
			measurement.setTrack(track);
			
			try {
				JSONObject json = new JSONObject(rawData);
				JSONArray names = json.names();
				if (names != null) {
					for (int j = 0; j < names.length(); j++) {
						String key = names.getString(j);
						measurement.addProperty(PropertyKey.valueOf(key), json.getDouble(key));
					}
				}
			} catch (JSONException e) {
				logger.severe("could not load properties", e);
			}
	
			allMeasurements.add(measurement);
			c.moveToNext();
		}
	
		c.close();
		return allMeasurements;
	}

}
