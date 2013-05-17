package car.io.adapter;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import car.io.exception.LocationInvalidException;

/**
 * Implementation of DbAdapter
 * 
 * @author jakob
 * 
 */

public class DbAdapterLocal implements DbAdapter {

	
	// Database tables

	public static final String KEY_TIME = "measurement_time";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_RPM = "rpm";
	public static final String KEY_SPEED = "speed";
	public static final String KEY_MAF = "maf";
	public static final String KEY_TRACK = "track";

	
	public static final String KEY_TRACK_NAME = "name";
	public static final String KEY_TRACK_DESCRIPTION = "descr";
	public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
	public static final String KEY_TRACK_CAR_MODEL = "car_model";
	public static final String KEY_TRACK_FUEL_TYPE = "fuel_type";
	public static final String KEY_TRACK_VIN = "vin";
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Database parameters

	private static final String DATABASE_NAME = "obd2";
	private static final int DATABASE_VERSION = 8;
	private static final String DATABASE_TABLE = "measurements";
	private static final String DATABASE_TABLE_TRACKS = "tracks";
	private static final String DATABASE_CREATE = "create table measurements "
			+ "(_id INTEGER primary key autoincrement, "
			+ "latitude BLOB, "
			+ "longitude BLOB, measurement_time BLOB, speed BLOB, maf BLOB, track TEXT);";
	private static final String DATABASE_CREATE_TRACK = "create table tracks"
			+" (_id INTEGER primary key autoincrement, "
			+"name BLOB, "
			+"descr BLOB, "
			+"car_manufacturer BLOB, "
			+"car_model BLOB, "
			+"fuel_type BLOB, "
			+"vin BLOB);";

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
			Log.w("obd2", "Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS measurements");
			db.execSQL("DROP TABLE IF EXISTS tracks");
			onCreate(db);
		}
	}

	public DbAdapterLocal(Context ctx) {
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
		mDbHelper.close();

	}

	@Override
	public void insertMeasurement(Measurement measurement) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_LATITUDE, measurement.getLatitude());
		initialValues.put(KEY_LONGITUDE, measurement.getLongitude());
		initialValues.put(KEY_TIME, measurement.getMeasurementTime());
		initialValues.put(KEY_SPEED, measurement.getSpeed());
		initialValues.put(KEY_MAF, measurement.getMaf());
		initialValues.put(KEY_TRACK,
				String.valueOf(measurement.getTrack().getId()));

		mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	

	private ArrayList<Measurement> getAllMeasurementsForTrack(Track track) {
		ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();
		
		Cursor c = mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME, KEY_SPEED,
				KEY_MAF },
				//null,
				KEY_TRACK+ "="+String.valueOf(track.getId()),
				null, null, null,
				//null
				KEY_TIME+" ASC"
				);

		c.moveToFirst();

		for (int i = 0; i < c.getCount(); i++) {

			String row = c.getString(0);
			String lat = c.getString(1);
			String lon = c.getString(2);
			String time = c.getString(3);
			String speed = c.getString(4);
			String maf = c.getString(5);
			//String track = c.getString(6); 

			try {
				Measurement measurement = new Measurement(Float.valueOf(lat),
						Float.valueOf(lon));
				measurement.setId(Integer.valueOf(row));
				measurement.setMeasurementTime(Long.valueOf(time));
				measurement.setSpeed(Integer.valueOf(speed));
				measurement.setMaf(Double.valueOf(maf));
				measurement.setTrack(track);

				allMeasurements.add(measurement);
			} catch (LocationInvalidException e) {
				Log.e("obd2", "gps not locked");
				e.printStackTrace();
			}

			c.moveToNext();
		}

		c.close();
		return allMeasurements;
	}
	
	@Override
	public Track getTrack(String id){
		Track t = new Track(id);
		
		Cursor c = mDb.query(DATABASE_TABLE_TRACKS, new String[] { 
				KEY_ROWID, 
				KEY_TRACK_NAME,
				KEY_TRACK_DESCRIPTION,
				KEY_TRACK_CAR_MANUFACTURER,
				KEY_TRACK_CAR_MODEL,
				KEY_TRACK_FUEL_TYPE,
				KEY_TRACK_VIN}, KEY_ROWID+ " = "+id, null, null, null, null);

		c.moveToFirst();
		
		t.setName(c.getString(0));
		t.setDescription(c.getString(1));
		t.setCarManufacturer(c.getString(2));
		t.setCarModel(c.getString(3));
		t.setFuelType(c.getString(4));
		t.setVin(c.getString(5));
		
		c.close();
		
		t.insertMeasurement(getAllMeasurementsForTrack(t));
		return t;
	}

	@Override
	public void deleteAllTracks() {
		mDb.delete(DATABASE_TABLE, null, null);
		mDb.delete(DATABASE_TABLE_TRACKS, null, null);
	}

	@Override
	public int getNumberOfStoredTracks() {
		ArrayList<Track> allTracks = getAllTracks();
		return allTracks.size();

	}

	@Override
	public long insertTrack(Track track) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_TRACK_NAME, track.getName());
		initialValues.put(KEY_TRACK_DESCRIPTION, track.getDescription());
		initialValues.put(KEY_TRACK_CAR_MANUFACTURER, track.getCarManufacturer());
		initialValues.put(KEY_TRACK_CAR_MODEL, track.getCarModel());
		initialValues.put(KEY_TRACK_FUEL_TYPE, track.getFuelType());
		initialValues.put(KEY_TRACK_VIN, track.getVin());
		
		return mDb.insert(DATABASE_TABLE_TRACKS, null, initialValues);
	}

	@Override
	public ArrayList<Track> getAllTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>();
		
		Cursor c = mDb.query(DATABASE_TABLE_TRACKS, null, null, null, null, null, null);
		//Cursor c = mDb.rawQuery("SELECT * from \"tracks\"", null);

		c.moveToFirst();
		
		for (int i = 0; i < c.getCount(); i++) {
			tracks.add(getTrack(c.getString(0)));
		}
		c.close();
		
		return tracks;
	}

}
