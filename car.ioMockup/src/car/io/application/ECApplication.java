package car.io.application;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import car.io.adapter.DbAdapter;
import car.io.adapter.DbAdapterLocal;
import car.io.adapter.Measurement;
import car.io.adapter.Track;
import car.io.commands.CommonCommand;
import car.io.commands.MAF;
import car.io.commands.RPM;
import car.io.commands.Speed;
import car.io.exception.LocationInvalidException;
import car.io.obd.BackgroundService;
import car.io.obd.Listener;
import car.io.obd.ServiceConnector;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ECApplication extends Application implements LocationListener {

	private static ECApplication singleton;
	private DbAdapter dbAdapter;
	private final ScheduledExecutorService scheduleTaskExecutor = Executors
			.newScheduledThreadPool(1);
	// get the default Bluetooth adapter
	private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	private ServiceConnector serviceConnector = null;
	private Intent backgroundService = null;
	private Handler handler = new Handler();
	private Listener listener = null;
	private LocationManager locationManager;

	private float locationLatitude;
	private float locationLongitude;
	private int speedMeasurement;
	private double mafMeasurement;
	private Measurement measurement = null;
	private long lastInsertTime = 0;

	private Track track;

	private boolean requirementsFulfilled = true;

	public ECApplication getInstance() {
		return singleton;
	}

	public ServiceConnector getServiceConnector() {
		return serviceConnector;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initDbAdapter();
		initBluetooth();
		initLocationManager();

		track = new Track("123456", "Gasoline", dbAdapter);

		try {
			measurement = new Measurement(locationLatitude, locationLongitude);
		} catch (LocationInvalidException e) {
			e.printStackTrace();
		}

		singleton = this;
	}

	private void initDbAdapter() {
		if (dbAdapter == null) {
			dbAdapter = new DbAdapterLocal(this.getApplicationContext());
			dbAdapter.open();
		}
	}

	private void initBluetooth() {
		if (bluetoothAdapter == null) {

			requirementsFulfilled = false;

		} else {

			if (!bluetoothAdapter.isEnabled()) {
				requirementsFulfilled = false;
			}
		}
	}

	public DbAdapter getDbAdapter() {
		return dbAdapter;
	}

	public void startBackgroundService() {
		if (requirementsFulfilled) {
			Log.e("obd2", "requirements met");
			backgroundService = new Intent(this, BackgroundService.class);
			serviceConnector = new ServiceConnector();
			serviceConnector.setServiceListener(listener);

			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		} else {
			Log.e("obd2", "requirements not met");
		}
	}

	public void startServiceConnector() {
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (requirementsFulfilled) {
					if (!serviceConnector.isRunning()) {
						startConnection();
					} else {
						Log.e("obd2", "serviceConnector not running");
					}
				} else {
					Log.e("obd2", "requirementsFulfilled was false!");
				}

			}
		}, 0, 1, TimeUnit.MINUTES);
	}

	public void startListener() {
		listener = new Listener() {

			public void receiveUpdate(CommonCommand job) {
				Log.e("obd2", "update received");
				// Get the name and the result of the Command

				String commandName = job.getCommandName();
				String commandResult = job.getResult();

				// Get the fuel type from the preferences

				// TextView fuelTypeTextView = (TextView)
				// findViewById(R.id.fueltypeText);
				// fuelTypeTextView.setText(preferences.getString(PREF_FUEL_TPYE,
				// "Gasoline"));

				/*
				 * Check which measurent is returned and save the value in the
				 * previously created measurement
				 */

				// Speed

				if (commandName.equals("Vehicle Speed")) {
					// TextView speedTextView = (TextView)
					// findViewById(R.id.spd_text);
					// speedTextView.setText(commandResult + " km/h");

					try {
						speedMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						Log.e("obd2", "speed parse exception");
						e.printStackTrace();
					}
				}

				// MAF

				else if (commandName.equals("Mass Air Flow")) {
					// TextView mafTextView = (TextView)
					// findViewById(R.id.mafText);
					String maf = commandResult;
					// mafTextView.setText("MAF: " + maf + " g/s");

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

				// Update and insert the measurement

				updateMeasurement();
			}

		};
	}

	public void stopServiceConnector() {
		scheduleTaskExecutor.shutdown();
	}

	/**
	 * Connects to the Bluetooth Adapter and starts the execution of the
	 * commands
	 */
	public void startConnection() {
		if (!serviceConnector.isRunning()) {
			Log.e("obd2", "service start");
			startService(backgroundService);
		}
		handler.post(waitingListRunnable);
	}

	/**
	 * Ends the connection with the Bluetooth Adapter
	 */
	public void stopConnection() {
		if (serviceConnector.isRunning())
			stopService(backgroundService);
		handler.removeCallbacks(waitingListRunnable);
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

	/**
	 * Helper method that adds the desired commands to the waiting list where
	 * all commands are executed
	 */
	private void addCommandstoWaitinglist() {
		final CommonCommand speed = new Speed(); // TODO take speed from
													// location provider
		final CommonCommand maf = new MAF();
		serviceConnector.addJobToWaitingList(speed);
		serviceConnector.addJobToWaitingList(maf);
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
				measurement.setMaf(mafMeasurement);
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

			track.addMeasurement(measurement2);

			Log.i("obd2", measurement2.toString());

			Toast.makeText(getApplicationContext(), measurement2.toString(),
					Toast.LENGTH_SHORT).show();

		}

	}

	/**
	 * Init the location Manager
	 */
	private void initLocationManager() {

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, this);

	}

	public void destroyStuff() {
		backgroundService = null;
		serviceConnector = null;
		listener = null;
		handler = null;
	}

	@Override
	public void onLocationChanged(Location location) {
		locationLatitude = (float) location.getLatitude();
		locationLongitude = (float) location.getLongitude();

	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}
