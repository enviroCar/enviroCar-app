package car.io.activity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerTabStrip;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.adapter.Measurement;
import car.io.adapter.Track;
import car.io.application.ECApplication;
import car.io.commands.CommonCommand;
import car.io.commands.MAF;
import car.io.commands.RPM;
import car.io.commands.Speed;
import car.io.exception.LocationInvalidException;
import car.io.obd.BackgroundService;
import car.io.obd.Listener;
import car.io.obd.ServiceConnector;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity<AndroidAlarmService> extends
		SwipeableFragmentActivity {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

	private ECApplication application;

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

	private boolean requirementsFulfilled = true;

	// Service objects

	private Handler handler = new Handler();
	private Listener listener = null;
	private Intent backgroundService = null;

	// Adapter Classes

	private DbAdapter dbAdapter;

	// Measurement values

	private float locationLatitude;
	private float locationLongitude;
	private int speedMeasurement;
	private double mafMeasurement;

	// Upload in Wlan

	private boolean uploadOnlyInWlan;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = ((ECApplication) getApplication()).getInstance();

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

		actionBar.setLogo(getResources().getDrawable(R.drawable.home_icon));

		addTab("My Tracks", ListMeasurementsFragment.class,
				MyData.createBundle("My Tracks"));
		addTab("OBD", OBDFrament.class, MyData.createBundle("Overview")); // TODO
																			// place
																			// controls
																			// located
																			// in
																			// main.xml
																			// to
																			// SettingsActivity
		addTab("Overview", DashboardFragment.class,
				MyData.createBundle("Overview"));
		addTab("Friends", ListFriends.class, MyData.createBundle("Friends"));
		// addTab( "Friends", MyData.class, MyData.createBundle( "Fragment 3")
		// );

		setSelectedTab(1);

		// --------------------------
		// --------------------------
		// --------------------------

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// AutoConnect checkbox and service

		// final CheckBox connectAutomatically = (CheckBox)
		// this.findViewById(R.id.checkBox1);
		//
		// connectAutomatically
		// .setOnCheckedChangeListener(new
		// CompoundButton.OnCheckedChangeListener() {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// if (connectAutomatically.isChecked()) { // Start Service
		// // every minute

		application.startServiceConnector();
		// } else { // Stop Service
		// application.stopServiceConnector();
		// }
		// }
		//
		// });

		// Toggle Button for WLan Upload
		/*
		 * final ToggleButton wlanToggleButton = (ToggleButton)
		 * findViewById(R.id.toggleButton1); wlanToggleButton.setChecked(true);
		 * uploadOnlyInWlan = true;
		 * 
		 * wlanToggleButton .setOnCheckedChangeListener(new
		 * CompoundButton.OnCheckedChangeListener() {
		 * 
		 * @Override public void onCheckedChanged(CompoundButton buttonView,
		 * boolean isChecked) { if (wlanToggleButton.isChecked()) {
		 * uploadOnlyInWlan = true; } else { uploadOnlyInWlan = false; }
		 * 
		 * } });
		 */

		// Upload data every 10 minutes and only if there are more than 50
		// measurements stored in the database

		ScheduledExecutorService uploadTaskExecutor = Executors
				.newScheduledThreadPool(1);
		uploadTaskExecutor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				Log.e("obd2", "pre uploading");

				/*
				 * if (dbAdapter.getNumberOfStoredMeasurements() > 50) { if
				 * (uploadOnlyInWlan == true) { if (mWifi.isConnected()) { //
				 * TODO: upload Log.e("obd2", "uploading"); } } else { // TODO:
				 * upload Log.e("obd2", "uploading"); } }
				 */

			}
		}, 0, 10, TimeUnit.MINUTES);

		// Make a new listener to interpret the measurement values that are
		// returned
		Log.e("obd2", "init listener");
		application.startListener();

		// If everything is available, start the service connector and listener

		application.startBackgroundService();

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

	private void testMethode() {

		// TODO make this test method work completely

		dbAdapter.deleteAllTracks();
		Track track = new Track("123456", "Gasoline", dbAdapter);

		track.setName("Testname");
		track.setDescription("Testdescription");
		track.setCarManufacturer("carManufacturer");
		track.setCarModel("CarModel");
		track.commitTrackToDatabase();

		try {
			Measurement m1 = new Measurement(51.4f, 7.6f);
			Measurement m2 = new Measurement(53.3f, 6.3f);

			m1.setSpeed(100);
			m2.setSpeed(200);

			m1.setMaf(5.5);
			m2.setMaf(5.7);

			track.addMeasurement(m1);
			track.addMeasurement(m2);

			Log.i("obd2",
					"Number of stored tracks: "
							+ dbAdapter.getNumberOfStoredTracks());

			ArrayList<Track> trackList = dbAdapter.getAllTracks();

			for (Track t : trackList) {
				Log.i("obd2", "TRACKS");
				Log.i("obd2", "id: " + track.getId() + " ID: " + t.getId());
				Log.i("obd2",
						"name: " + track.getName() + " Name: " + t.getName());
				Log.i("obd2",
						"desc: " + track.getDescription() + " Desc: "
								+ t.getDescription());
				Log.i("obd2", "CMa: " + track.getCarManufacturer() + " CMa: "
						+ t.getCarManufacturer());
				Log.i("obd2",
						"CMo: " + track.getCarModel() + " CMo: "
								+ t.getCarModel());
				Log.i("obd2", "VIN: " + track.getVin() + " VIN: " + t.getVin());
				Log.i("obd2",
						"FT: " + track.getFuelType() + " FT: "
								+ t.getFuelType());
				Measurement m3 = t.getMeasurements().get(0);
				Measurement m4 = t.getMeasurements().get(1);
				Log.i("obd2", "MEASUREMENTS");
				Log.i("obd2", "m1 id: " + m1.getId() + " m3 id: " + m3.getId());
				Log.i("obd2", "m2 id: " + m2.getId() + " m4 id: " + m4.getId());
				Log.i("obd2",
						"m1 lat: " + m1.getLatitude() + " m3 lat: "
								+ m3.getLatitude());
				Log.i("obd2",
						"m2 lat: " + m2.getLatitude() + " m4 lat: "
								+ m4.getLatitude());
				Log.i("obd2",
						"m1 lon: " + m1.getLongitude() + " m3 lon: "
								+ m3.getLongitude());
				Log.i("obd2",
						"m2 lon: " + m2.getLongitude() + " m4 lon: "
								+ m4.getLongitude());
				Log.i("obd2", "m1 mt: " + m1.getMeasurementTime() + " m3 mt: "
						+ m3.getMeasurementTime());
				Log.i("obd2", "m2 mt: " + m2.getMeasurementTime() + " m4 mt: "
						+ m4.getMeasurementTime());
				Log.i("obd2",
						"m1 speed: " + m1.getSpeed() + " m3 speed: "
								+ m3.getSpeed());
				Log.i("obd2",
						"m2 speed: " + m2.getSpeed() + " m4 speed: "
								+ m4.getSpeed());
				Log.i("obd2",
						"m1 maf: " + m1.getMaf() + " m3 maf: " + m3.getMaf());
				Log.i("obd2",
						"m2 maf: " + m2.getMaf() + " m4 maf: " + m4.getMaf());
				Log.i("obd2", "m1 track: " + m1.getTrack().getId()
						+ " m3 track: " + m3.getTrack().getId());
				Log.i("obd2", "m2 track: " + m2.getTrack().getId()
						+ " m4 track: " + m4.getTrack().getId());
			}

		} catch (LocationInvalidException e) {
			e.printStackTrace();
		}
		//dbAdapter.deleteAllTracks();
	}

	// -----------------------------------------------------------

	/**
	 * Helper method that inits the DbAdapter
	 */
	private void initDbAdapter() {
		dbAdapter = (DbAdapter) application.getDbAdapterLocal();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

		dbAdapter.close();

		// Remove the services etc.

		application.destroyStuff();

	}

	@Override
	protected void onPause() {
		super.onPause();

		// Stop GPS

		application.stopLocating();

		// Close DB

		dbAdapter.close();
	}

	protected void onResume() {
		super.onResume();

		initDbAdapter();

		// ---TESTMETHODE
		testMethode();
		// ---TESTMETHODE

		// Update preferences

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		application.downloadTracks();
	}

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
			application.startConnection();
			return true;

		case STOP_MEASUREMENT:
			application.stopConnection();
			return true;

		case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
			return true;

			// case START_LIST_VIEW:
			// Intent listIntent = new Intent(this,
			// ListMeasurementsActivity.class);
			// startActivity(listIntent);
			// return true;
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
			if (application.getServiceConnector().isRunning()) {
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
}
