package car.io.activity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerTabStrip;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.UploadManager;
import car.io.application.ECApplication;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity<AndroidAlarmService> extends
		SwipeableFragmentActivity {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

	public ECApplication application;

	// Menu Items

	static final int NO_BLUETOOTH = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_GPS = 2;
	static final int START_STOP_MEASUREMENT = R.id.menu_start;
	static final int SETTINGS = R.id.menu_settings;
	static final int LOGIN = R.id.menu_login;
	static final int REMOVE_LOCAL_TRACKS = R.id.menu_about;
	static final int START_UPLOAD = R.id.menu_upload;
	static final int MENU_GARAGE = R.id.menu_my_garage;

	// Properties

	// private static final String PREF_FUEL_TPYE = "pref_fuel_type";
	// private static final String PREF_CAR_TYPE = "car_preference";

	// private boolean requirementsFulfilled = true;

	// Service objects

	// private Handler handler = new Handler();
	// private Listener listener = null;
	// private Intent backgroundService = null;

	// Adapter Classes

	// private DbAdapter dbAdapter;

	// Measurement values

	// private float locationLatitude;
	// private float locationLongitude;
	// private int speedMeasurement;
	// private double mafMeasurement;

	// Upload in Wlan

	// private boolean uploadOnlyInWlan;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = ((ECApplication) getApplication());

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

		addTab("Local Tracks", ListMeasurementsFragmentLocal.class);
		addTab("My Tracks", ListMeasurementsFragment.class);
		// addTab("OBD", OBDFrament.class); // TODO
		// place
		// controls
		// located
		// in
		// main.xml
		// to
		// SettingsActivity
		addTab("Dashboard", DashboardFragment.class);

		setSelectedTab(2);

		// --------------------------
		// --------------------------
		// --------------------------


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

		// application.startServiceConnector();
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

		// Button closeButton = (Button) findViewById(R.id.uploadnow);
		// closeButton.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// }
		// });

		// initDbAdapter();
		// testMethode();

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

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

		application.closeDb();

		// Remove the services etc.

		application.destroyStuff();

	}

	@Override
	protected void onPause() {
		super.onPause();

		// Stop GPS

		// application.stopLocating();

		// Close DB

		// application.closeDb();
	}

	/**
	 * Determine what the menu buttons do
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case START_STOP_MEASUREMENT:
			if(!application.getServiceConnector().isRunning()){
				application.startConnection();
			}else {
				application.stopConnection();
			}
			return true;

		case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
			return true;

		case LOGIN:
			Intent loginIntent = new Intent(this, LoginActivity.class);
			startActivity(loginIntent);
			return true;

		case START_UPLOAD:
			UploadManager uploadManager = new UploadManager(
					application.getDbAdapterLocal(), getApplication());
			uploadManager.uploadAllTracks();
			return true;

		case REMOVE_LOCAL_TRACKS:
			// dbAdapter.deleteAllTracks();
			application.getDbAdapterLocal().deleteAllTracks();
			application.setTrack(null);
			Log.i("obd2", "deleted all local tracks");
			return true;

		case MENU_GARAGE:
			Intent garageIntent = new Intent(this, MyGarage.class);
			startActivity(garageIntent);
			return true;
		}
		return false;
	}

	/**
	 * Activate or deactivate the menu items
	 */
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem startStop = menu.findItem(START_STOP_MEASUREMENT);
		MenuItem settings = menu.findItem(SETTINGS);
		MenuItem upload = menu.findItem(START_UPLOAD);
		MenuItem myGarage = menu.findItem(MENU_GARAGE);
		MenuItem loginRegister = menu.findItem(LOGIN);

		if (application.requirementsFulfilled()) { // was requirementsFulfilled
			try {
				if (application.getServiceConnector().isRunning()) {
					startStop.setTitle(R.string.menu_stop);
					//stop.setEnabled(true);
					settings.setEnabled(false);
				} else {
					startStop.setTitle(R.string.menu_start);

					// Only enable start button when adapter is selected

					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(this);

					String remoteDevice = preferences.getString(
							car.io.activity.SettingsActivity.BLUETOOTH_KEY,
							null);

					if (remoteDevice != null) {
						startStop.setEnabled(true);
					} else {
						startStop.setEnabled(false);
					}
					settings.setEnabled(true);
				}
			} catch (NullPointerException e) {
				Log.e("obd2", "The Service Connector is null.");
				startStop.setEnabled(false);
				settings.setEnabled(true);
				e.printStackTrace();
			}
		} else {
			startStop.setTitle(R.string.menu_start);
			startStop.setEnabled(false);
			settings.setEnabled(false);
		}

		if (application.getDbAdapterLocal().getAllTracks().size() > 0
				&& application.isLoggedIn()) {
			upload.setEnabled(true);
		} else {
			upload.setEnabled(false);
		}

		if (application.isLoggedIn()) {
			myGarage.setEnabled(true);
			loginRegister.setTitle(String.format(
					getResources().getString(R.string.logged_in_as),
					application.getUser().getUsername()));
		} else {
			myGarage.setEnabled(false);
		}

		return true;
	}
}
