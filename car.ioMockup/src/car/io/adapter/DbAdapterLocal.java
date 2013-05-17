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
	// public static final String KEY_THROTTLE = "throttle_position";
	public static final String KEY_RPM = "rpm";
	public static final String KEY_SPEED = "speed";
	// public static final String KEY_FUELTYPE = "fuel_type";
	// public static final String KEY_ENGINELOAD = "engine_load";
	// public static final String KEY_FUELCONSUMPTION = "fuel_consumption";
	// public static final String KEY_INTAKEPRESSURE = "intake_pressure";
	// public static final String KEY_INTAKETEMPERATURE = "intake_temperature";
	// public static final String KEY_SHORTTERMTRIMBANK1 =
	// "short_term_trim_bank_1";
	// public static final String KEY_LONGTERMTRIMBANK1 =
	// "long_term_trim_bank_1";
	public static final String KEY_MAF = "maf";
	// public static final String KEY_CAR = "car";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Database parameters

	private static final String DATABASE_NAME = "obd2";
	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_TABLE = "measurements";
	private static final String DATABASE_CREATE = "create table measurements "
			+ "(_id INTEGER primary key autoincrement, "
			+ "latitude BLOB, "
			+ "longitude BLOB, measurement_time BLOB,  rpm BLOB, speed BLOB, maf BLOB);";

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
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("obd2", "Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS measurements");
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
		// initialValues.put(KEY_THROTTLE, measurement.getThrottlePosition());
		initialValues.put(KEY_RPM, measurement.getRpm());
		initialValues.put(KEY_SPEED, measurement.getSpeed());
		// initialValues.put(KEY_FUELTYPE, measurement.getFuelType());
		// initialValues.put(KEY_ENGINELOAD, measurement.getEngineLoad());
		// initialValues
		// .put(KEY_FUELCONSUMPTION, measurement.getFuelConsumption());
		// initialValues.put(KEY_INTAKEPRESSURE,
		// measurement.getIntakePressure());
		// initialValues.put(KEY_INTAKETEMPERATURE,
		// measurement.getIntakeTemperature());
		// initialValues.put(KEY_SHORTTERMTRIMBANK1,
		// measurement.getShortTermTrimBank1());
		// initialValues.put(KEY_LONGTERMTRIMBANK1,
		// measurement.getLongTermTrimBank1());
		initialValues.put(KEY_MAF, measurement.getMaf());
		// initialValues.put(KEY_CAR, measurement.getCar());

		mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	@Override
	public ArrayList<Measurement> getAllMeasurements() {
		ArrayList<Measurement> allMeasurements = new ArrayList<Measurement>();

		Cursor c = mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME, KEY_RPM, KEY_SPEED,
				KEY_MAF }, null, null, null, null, null);

		c.moveToFirst();

		for (int i = 0; i < c.getCount(); i++) {

			String row = c.getString(0);
			String lat = c.getString(1);
			String lon = c.getString(2);
			String time = c.getString(3);
			String rpm = c.getString(4);
			String speed = c.getString(5);
			String maf = c.getString(6);

			try {
				Measurement measurement = new Measurement(Float.valueOf(lat),
						Float.valueOf(lon));
				measurement.setId(Integer.valueOf(row));
				measurement.setMeasurementTime(Long.valueOf(time));
				measurement.setRpm(Integer.valueOf(rpm));
				measurement.setSpeed(Integer.valueOf(speed));
				measurement.setMaf(Double.valueOf(maf));

				allMeasurements.add(measurement);
			} catch (LocationInvalidException e) {
				Log.e("obd2", "gps not locked");
				e.printStackTrace();
			}

			c.moveToNext();
		}

		return allMeasurements;
	}

	@Override
	public Measurement getMeasurement(int id) {
		ArrayList<Measurement> allMeasurements = getAllMeasurements();

		for (Measurement measurement : allMeasurements) {
			if (id == measurement.getId()) {
				return measurement;
			}
		}

		return null;
	}

	@Override
	public void deleteAllMeasurements() {
		mDb.delete(DATABASE_TABLE, null, null);

	}

	@Override
	public int getNumberOfStoredMeasurements() {

		ArrayList<Measurement> allMeasurements = getAllMeasurements();

		return allMeasurements.size();

	}

}
