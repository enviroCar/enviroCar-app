package de.ifgi.car.io.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
import com.ifgi.obd2.obd.Listener;
import com.ifgi.obd2.obd.ServiceConnector;

import de.ifgi.car.io.R;

public class MainActivity extends SwipeableFragmentActivity implements
		LocationListener {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

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

	// Upload in Wlan

	private boolean uploadOnlyInWlan;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int pageMargin = (int) (4 * getResources().getDisplayMetrics().density);
		viewPager.setPageMargin(pageMargin);
		viewPager.setPageMarginDrawable(R.drawable.viewpager_margin);

		actionBar = getSupportActionBar();
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);

		((PagerTabStrip) this.findViewById(R.id.pager_title_strip))
				.setTabIndicatorColorResource(R.color.blue_light_cario);

		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}
		View rootView = findViewById(R.id.pager_title_strip);
		TYPEFACE.applyCustomFont((ViewGroup) rootView, TYPEFACE.Newscycle(this));

		addTab("List", ListMeasurementsFragment.class,
				MyData.createBundle("Overview"));
		addTab( "OBD", OBDFrament.class, MyData.createBundle( "Overview") );
		// addTab( "Friends", MyData.class, MyData.createBundle( "Fragment 3")
		// );

		setSelectedTab(1);
		// --------------------------
		// --------------------------
		// --------------------------
		

		
		
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getSupportMenuInflater().inflate(R.menu.menu, menu); return true; }
	 */
	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) {
	 * Log.i("itemid",item.getItemId()+""); switch(item.getItemId()){ case
	 * R.id.menu_help: startActivity(new
	 * Intent(MainActivity.this,CheckListActivity.class)); break; case
	 * R.id.menu_settings: startActivity(new
	 * Intent(MainActivity.this,SettingsActivity.class)); break; } return true;
	 * }
	 */

	// -----------------------------------------------------------
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
			startConnection();
			return true;

		case STOP_MEASUREMENT:
			stopConnection();
			return true;

		case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
			return true;

			/*
			 * case START_LIST_VIEW: Intent listIntent = new Intent(this,
			 * ListMeasurementsActivity.class); startActivity(listIntent);
			 * return true;
			 */
		}
		return false;
	}

	/**
	 * Connects to the Bluetooth Adapter and starts the execution of the
	 * commands
	 */
	public void startConnection() {
		if(serviceConnector == null){
			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("OBDFragment");
			serviceConnector = ((OBDFrament) currentFragment).getServiceConnector();
		}
		if (!serviceConnector.isRunning()) {

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
	 * Activate or deactivate the menu items
	 */
	public boolean onPrepareOptionsMenu(Menu menu) {
/*
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
*/
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
