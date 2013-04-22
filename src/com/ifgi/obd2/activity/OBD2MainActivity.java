package com.ifgi.obd2.activity;

import java.text.NumberFormat;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.ifgi.obd2.R;
import com.ifgi.obd2.adapter.DbAdapter;
import com.ifgi.obd2.adapter.DbAdapterLocal;
import com.ifgi.obd2.adapter.Measurement;
import com.ifgi.obd2.commands.CommonCommand;
import com.ifgi.obd2.commands.EngineLoad;
import com.ifgi.obd2.commands.IntakePressure;
import com.ifgi.obd2.commands.IntakeTemperature;
import com.ifgi.obd2.commands.LongTermTrimBank1;
import com.ifgi.obd2.commands.MAF;
import com.ifgi.obd2.commands.RPM;
import com.ifgi.obd2.commands.ShortTermTrimBank1;
import com.ifgi.obd2.commands.Speed;
import com.ifgi.obd2.commands.TPS;
import com.ifgi.obd2.exception.LocationInvalidException;
import com.ifgi.obd2.obd.BackgroundService;
import com.ifgi.obd2.obd.Listener;
import com.ifgi.obd2.obd.ServiceConnector;

/**
 * Main Activity for the app. Press MENU to start or stop the
 * measurement-process or to access the measurements list or the settings. You
 * have to enter the settings before the first measurement. The devices have to
 * be paired before you can start the measurement
 * 
 * @author jakob
 * 
 */

public class OBD2MainActivity extends Activity implements LocationListener {

	// Preferences

	private SharedPreferences preferences = null;

	// Menu Items

	static final int NO_BLUETOOTH = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_GPS = 2;
	static final int START_MEASUREMENT = 3;
	static final int STOP_MEASUREMENT = 4;
	static final int SETTINGS = 5;
	static final int START_LIST_VIEW = 6;

	// Properties

	private static final String PREF_FUEL_TPYE = "pref_fuel_type";
	private static final String PREF_CAR_TYPE = "car_preference";
	private long lastInsertTime = 0;
	private boolean requirementsFulfilled = true;

	// Service objects

	private Handler handler = new Handler();
	private Listener listener = null;
	private Intent backgroundService = null;
	private ServiceConnector serviceConnector = null;

	// Adapter Classes

	private Measurement measurement = null;
	private LocationManager locationManager;
	private DbAdapter dbAdapter;

	// Measurement values

	private float locationLatitude;
	private float locationLongitude;
	private int speedMeasurement;
	private int rpmMeasurement;
	private double throttlePositionMeasurement;
	private double shortTermTrimBank1Measurement;
	private double longTermTrimBank1Measurement;
	private int intakePressureMeasurement;
	private int intakeTemperatureMeasurement;
	private double fuelConsumptionMeasurement;
	private double mafMeasurement;
	private double engineLoadMeasurement;

	// TestViews

	private TextView locationLatitudeTextView;
	private TextView locationLongitudeTextView;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		/*
		 * Init
		 */

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		locationLatitudeTextView = (TextView) findViewById(R.id.latitudeText);
		locationLongitudeTextView = (TextView) findViewById(R.id.longitudeText);

		/*
		 * Measure with the listener
		 */

		// Try to create an empty measurement

		try {
			measurement = new Measurement(locationLatitude, locationLongitude);
		} catch (LocationInvalidException e) {
			e.printStackTrace();
		}

		// Make a new listener to interpret the measurement values that are
		// returned

		listener = new Listener() {

			public void receiveUpdate(CommonCommand job) {

				// Get the name and the result of the Command

				String commandName = job.getCommandName();
				String commandResult = job.getResult();

				// Get the fuel type from the preferences

				TextView fuelTypeTextView = (TextView) findViewById(R.id.fueltypeText);
				fuelTypeTextView.setText(preferences.getString(PREF_FUEL_TPYE,
						"Gasoline"));

				/*
				 * Check which measurent is returned and save the value in the
				 * previously created measurement
				 */

				// RPM

				if (commandName.equals("Engine RPM")) {
					TextView rpmTextView = (TextView) findViewById(R.id.rpm_text);
					rpmTextView.setText(commandResult + " rpm");
					rpmMeasurement = Integer.valueOf(commandResult);

				}

				// Speed

				else if (commandName.equals("Vehicle Speed")) {
					TextView speedTextView = (TextView) findViewById(R.id.spd_text);
					speedTextView.setText(commandResult + " km/h");

					try {
						speedMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						Log.e("obd2", "speed parse exception");
						e.printStackTrace();
					}
				}

				// Short Term Trim Bank 1

				else if (commandName.equals("Short Term Fuel Trim Bank 1")) {
					TextView shortTermTrimTextView = (TextView) findViewById(R.id.shortTrimText);
					String shortTermTrimBank1 = commandResult;
					shortTermTrimTextView.setText("Short Term Trim: "
							+ shortTermTrimBank1 + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(shortTermTrimBank1);
						shortTermTrimBank1Measurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception short term");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception short term");
						e.printStackTrace();
					}
				}

				// Long Term Trim Bank 1

				else if (commandName.equals("Long Term Fuel Trim Bank 1")) {
					TextView longTermTrimTextView = (TextView) findViewById(R.id.longTrimText);
					String longTermTrimBank1 = commandResult;
					longTermTrimTextView.setText("Long Term Trim: "
							+ longTermTrimBank1 + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(longTermTrimBank1);
						longTermTrimBank1Measurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception long term");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception long term");
						e.printStackTrace();
					}
				}

				// Intake Temperature

				else if (commandName.equals("Air Intake Temperature")) {
					TextView intakeTempTextView = (TextView) findViewById(R.id.intakeTempText);
					String intakeTemperature = commandResult;
					intakeTempTextView.setText("Intake Temp: "
							+ intakeTemperature + " C");
					try {
						intakeTemperatureMeasurement = Integer
								.valueOf(intakeTemperature);
					} catch (NumberFormatException e) {
						Log.e("obd2", "intake temp parse exception");
						e.printStackTrace();
					}

				}

				// Throttle Position

				else if (commandName.equals("Throttle Position")) {
					TextView throttlePositionTextView = (TextView) findViewById(R.id.throttle);
					String throttlePosition = commandResult;
					throttlePositionTextView.setText("T. Pos: "
							+ throttlePosition + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(throttlePosition);
						throttlePositionMeasurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception throttle");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception throttle");
						e.printStackTrace();
					}

				}

				// Engine Load

				else if (commandName.equals("Engine Load")) {
					TextView engineLoadTextView = (TextView) findViewById(R.id.engineLoadText);
					String engineLoad = commandResult;
					Log.e("obd2", "Engine Load: " + engineLoad);
					engineLoadTextView.setText("Engine load: " + engineLoad
							+ " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(engineLoad);
						engineLoadMeasurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception load");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception load");
						e.printStackTrace();
					}
				}

				// MAF

				else if (commandName.equals("Mass Air Flow")) {
					TextView mafTextView = (TextView) findViewById(R.id.mafText);
					String maf = commandResult;
					mafTextView.setText("MAF: " + maf + " g/s");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(maf);
						mafMeasurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd", "parse exception maf");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd", "parse exception maf");
						e.printStackTrace();
					}
				}

				// Intake Pressure

				else if (commandName.equals("Intake Manifold Pressure")) {
					TextView intakePressureTextView = (TextView) findViewById(R.id.intakeText);
					String intakePressure = commandResult;
					intakePressureTextView.setText("Intake: " + intakePressure
							+ "kPa");

					try {
						intakePressureMeasurement = Integer
								.valueOf(intakePressure);
					} catch (NumberFormatException e) {
						Log.e("obd", "intake pressure parse exception");
						e.printStackTrace();
					}
				}

				// Update and insert the measurement

				updateMeasurement();
			}

		};

		// Get the default bluetooth adapter

		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		// Check whether the bluetooth adapter is available or supported

		if (bluetoothAdapter == null) {

			requirementsFulfilled = false;
			showDialog(NO_BLUETOOTH);

		} else {

			if (!bluetoothAdapter.isEnabled()) {
				requirementsFulfilled = false;
				showDialog(BLUETOOTH_DISABLED);
			}
		}

		// If everything is available, start the service connector and listener

		if (requirementsFulfilled) {
			backgroundService = new Intent(this, BackgroundService.class);
			serviceConnector = new ServiceConnector();
			serviceConnector.setServiceListener(listener);

			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		}
	}

	/**
	 * Helper method that adds the desired commands to the waiting list where
	 * all commands are executed
	 */
	private void addCommandstoWaitinglist() {
		final CommonCommand speed = new Speed();
		final CommonCommand rpm = new RPM();
		final CommonCommand ltft1 = new LongTermTrimBank1();
		final CommonCommand stft1 = new ShortTermTrimBank1();
		final CommonCommand throttle = new TPS();
		final CommonCommand engineLoad = new EngineLoad();
		final CommonCommand maf = new MAF();
		final CommonCommand intakePressure = new IntakePressure();
		final CommonCommand intakeTemperature = new IntakeTemperature();

		serviceConnector.addJobToWaitingList(speed);
		serviceConnector.addJobToWaitingList(rpm);
		serviceConnector.addJobToWaitingList(ltft1);
		serviceConnector.addJobToWaitingList(throttle);
		serviceConnector.addJobToWaitingList(engineLoad);
		serviceConnector.addJobToWaitingList(maf);
		serviceConnector.addJobToWaitingList(intakePressure);
		serviceConnector.addJobToWaitingList(stft1);
		serviceConnector.addJobToWaitingList(intakeTemperature);
	}

	/**
	 * Helper Command that updates the current measurement with the last
	 * measurement data and inserts it into the database if the measurements is
	 * young enough
	 */
	public void updateMeasurement() {

		// Create a new measurement if necessary

		if (measurement == null) {
			try {
				measurement = new Measurement(locationLatitude,
						locationLongitude);
			} catch (LocationInvalidException e) {
				e.printStackTrace();
			}
		}

		// Insert the values if the measurement (with the coordinates) is young
		// enough (5000ms) or create a new one if it is too old

		if (measurement != null) {

			if (Math.abs(measurement.getMeasurementTime()
					- System.currentTimeMillis()) < 5000) {

				measurement.setSpeed(speedMeasurement);
				measurement.setRpm(rpmMeasurement);
				measurement.setThrottlePosition(throttlePositionMeasurement);
				measurement.setEngineLoad(engineLoadMeasurement);
				measurement.setFuelConsumption(fuelConsumptionMeasurement);
				measurement.setIntakePressure(intakePressureMeasurement);
				measurement.setIntakeTemperature(intakeTemperatureMeasurement);
				measurement
						.setShortTermTrimBank1(shortTermTrimBank1Measurement);
				measurement.setLongTermTrimBank1(longTermTrimBank1Measurement);
				measurement.setMaf(mafMeasurement);
				measurement.setFuelType(preferences.getString(PREF_FUEL_TPYE,
						"Gasoline"));
				measurement.setCar(preferences.getString(PREF_CAR_TYPE,
						"Not specified"));
				Log.e("obd2", "new measurement");
				Log.e("obd2", measurement.toString());

				insertMeasurement(measurement);

			} else {
				try {
					measurement = new Measurement(locationLatitude,
							locationLongitude);
				} catch (LocationInvalidException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Helper method to insert a measurement into the database (ensures that a
	 * measurement is only stored every 5 seconds and not faster...)
	 * 
	 * @param measurement2
	 *            The measurement you want to insert
	 */
	private void insertMeasurement(Measurement measurement2) {

		if (Math.abs(lastInsertTime - measurement2.getMeasurementTime()) > 5000) {

			lastInsertTime = measurement2.getMeasurementTime();

			dbAdapter.insertMeasurement(measurement2);

			Toast.makeText(getApplicationContext(), measurement2.toString(),
					Toast.LENGTH_SHORT).show();

		}

	}

	/**
	 * Helper method that inits the DbAdapter
	 */
	private void initDbAdapter() {
		dbAdapter = new DbAdapterLocal(getApplicationContext());
		dbAdapter.open();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

		dbAdapter.close();

		// Remove the services etc.

		backgroundService = null;
		serviceConnector = null;
		listener = null;
		handler = null;

	}

	@Override
	protected void onPause() {
		super.onPause();

		// Stop GPS

		locationManager.removeUpdates(this);

		// Close DB

		dbAdapter.close();
	}

	/**
	 * Init the location Manager with the user's choice of the location method
	 */
	private void initLocationManager() {

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, this);

	}

	protected void onResume() {
		super.onResume();

		initLocationManager();

		initDbAdapter();

		// doTests();

		// Update preferences

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	// private void doTests() {
	// try {
	// Measurement testMeasurement = new Measurement(52.0f, 7.0f);
	//
	// testMeasurement.setEngineLoad(10.0);
	//
	// insertMeasurement(testMeasurement);
	// } catch (LocationInvalidException e) {
	// e.printStackTrace();
	// }
	//
	// }

	/**
	 * Create the menu with the entries
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, START_MEASUREMENT, 0, "Start");
		menu.add(0, STOP_MEASUREMENT, 0, "Stop");
		menu.add(0, START_LIST_VIEW, 0, "List");
		menu.add(0, SETTINGS, 0, "Settings");
		return true;
	}

	/**
	 * Determine what the menu buttons do
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case START_MEASUREMENT:
			if (!serviceConnector.isRunning()) {

				// TODO at this point, insert a new service:

				/**
				 * We need a new service that runs in the background. This
				 * service has to start the backgroundService every X minutes.
				 * If the backgroundService could be started, we are connected
				 * to the car. If the backgroundService could not be started,
				 * the device is not in range. Then, the new service has to
				 * retry to start the service in X minutes.
				 */

				startService(backgroundService);
			}
			handler.post(waitingListRunnable);
			return true;

		case STOP_MEASUREMENT:
			if (serviceConnector.isRunning())
				stopService(backgroundService);
			handler.removeCallbacks(waitingListRunnable);
			return true;

		case SETTINGS:
			Intent configIntent = new Intent(this, Settings.class);
			startActivity(configIntent);
			return true;

		case START_LIST_VIEW:
			Intent listIntent = new Intent(this, ListMeasurementsActivity.class);
			startActivity(listIntent);
			return true;
		}
		return false;
	}

	/**
	 * Activate or deactivate the menu items
	 */
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem start = menu.findItem(START_MEASUREMENT);
		MenuItem stop = menu.findItem(STOP_MEASUREMENT);
		MenuItem settings = menu.findItem(SETTINGS);

		if (requirementsFulfilled) {
			if (serviceConnector.isRunning()) {
				start.setEnabled(false);
				stop.setEnabled(true);
				settings.setEnabled(false);
			} else {
				stop.setEnabled(false);
				start.setEnabled(true);
				settings.setEnabled(true);
			}
		} else {
			start.setEnabled(false);
			stop.setEnabled(false);
			settings.setEnabled(false);
		}

		return true;
	}

	/**
	 * Handles the waiting-list
	 */
	private Runnable waitingListRunnable = new Runnable() {
		public void run() {

			if (serviceConnector.isRunning())
				addCommandstoWaitinglist();

			handler.postDelayed(waitingListRunnable, 2000);
		}
	};

	@Override
	public void onLocationChanged(Location location) {

		locationLatitude = (float) location.getLatitude();

		locationLatitudeTextView.setText(String.valueOf(locationLatitude));

		locationLongitude = (float) location.getLongitude();

		locationLongitudeTextView.setText(String.valueOf(locationLongitude));

	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(getApplicationContext(), "Gps Disabled",
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(getApplicationContext(), "Gps Enabled",
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

}