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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.UploadManager;
import car.io.application.ECApplication;
import car.io.application.NavMenuItem;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;


public class MainActivity<AndroidAlarmService> extends SherlockFragmentActivity implements OnItemClickListener {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

	public ECApplication application;

	private FragmentManager manager;
	private DrawerLayout drawer;
	private ListView drawerList;

	
	private NavMenuItem[] navDrawerItems;
	// Menu Items

	static final int NO_BLUETOOTH = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_GPS = 2;
	static final int START_STOP_MEASUREMENT = 3;
	static final int SETTINGS = 6;
	static final int LOGIN = 0;
	static final int REMOVE_LOCAL_TRACKS = 4;
	static final int START_UPLOAD = 5;
	static final int MENU_GARAGE = 2;
	static final int MY_TRACKS = 1;

	public static final int REQUEST_MY_GARAGE = 1336;
	public static final int REQUEST_REDIRECT_TO_GARAGE = 1337;


	// Upload in Wlan

	// private boolean uploadOnlyInWlan;
	
	private void prepareNavDrawerItems(){
		if(this.navDrawerItems == null){
			navDrawerItems = new NavMenuItem[7];
			navDrawerItems[LOGIN] = new NavMenuItem(LOGIN, getResources().getString(R.string.menu_login),R.drawable.home_icon);
			navDrawerItems[SETTINGS] = new NavMenuItem(SETTINGS, getResources().getString(R.string.menu_settings),R.drawable.home_icon);
			navDrawerItems[START_STOP_MEASUREMENT] = new NavMenuItem(START_STOP_MEASUREMENT, getResources().getString(R.string.menu_start),R.drawable.home_icon);
			navDrawerItems[START_UPLOAD] = new NavMenuItem(START_UPLOAD, getResources().getString(R.string.menu_upload),R.drawable.home_icon);
			navDrawerItems[MENU_GARAGE] = new NavMenuItem(MENU_GARAGE, getResources().getString(R.string.menu_garage),R.drawable.home_icon);
			navDrawerItems[REMOVE_LOCAL_TRACKS] = new NavMenuItem(REMOVE_LOCAL_TRACKS, getResources().getString(R.string.menu_delete),R.drawable.home_icon);
			navDrawerItems[MY_TRACKS] = new NavMenuItem(MY_TRACKS, "My Tracks",R.drawable.home_icon);
		}
		
		if (application.requirementsFulfilled()) { // was requirementsFulfilled
		try {
			if (application.getServiceConnector().isRunning()) {
				navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_stop));
				// stop.setEnabled(true);
				navDrawerItems[SETTINGS].setEnabled(false);
			} else {
				navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));

				// Only enable start button when adapter is selected

				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(this);

				String remoteDevice = preferences.getString(
						car.io.activity.SettingsActivity.BLUETOOTH_KEY,
						null);

				if (remoteDevice != null) {
					navDrawerItems[START_STOP_MEASUREMENT].setEnabled(true);
				} else {
					navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
				}
				navDrawerItems[SETTINGS].setEnabled(true);
			}
		} catch (NullPointerException e) {
			Log.e("obd2", "The Service Connector is null.");
			navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
			navDrawerItems[SETTINGS].setEnabled(true);
			e.printStackTrace();
		}
	} else {
		navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));
		navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
		navDrawerItems[SETTINGS].setEnabled(false);
	}

	if (application.getDbAdapterLocal().getAllTracks().size() > 0
			&& application.isLoggedIn()) {
		navDrawerItems[START_UPLOAD].setEnabled(true);
	} else {
		navDrawerItems[START_UPLOAD].setEnabled(false);
	}

	if (application.isLoggedIn()) {
		navDrawerItems[MENU_GARAGE].setEnabled(true);
		navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_logout));
		navDrawerItems[LOGIN].setSubtitle(String.format(getResources().getString(R.string.logged_in_as),application.getUser().getUsername()));
	} else {
		navDrawerItems[MENU_GARAGE].setEnabled(false);
		navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_login));
		navDrawerItems[LOGIN].setSubtitle("");		
	}


	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main_layout);

		application = ((ECApplication) getApplication());

		// int pageMargin = (int) (4 *
		// getResources().getDisplayMetrics().density);
		// viewPager.setPageMargin(pageMargin);
		// viewPager.setPageMarginDrawable(R.drawable.viewpager_margin);

		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("");
		// actionBar.setH

		// ((PagerTabStrip) this.findViewById(R.id.pager_title_strip))
		// .setTabIndicatorColorResource(R.color.blue_light_cario);

		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}
		// View rootView = findViewById(R.id.pager_title_strip);
		// TYPEFACE.applyCustomFont((ViewGroup) rootView,
		// TYPEFACE.Newscycle(this));

		actionBar.setLogo(getResources().getDrawable(R.drawable.home_icon));

		// addTab("Local Tracks", ListMeasurementsFragmentLocal.class);
		// addTab("My Tracks", ListMeasurementsFragment.class);
		// addTab("OBD", OBDFrament.class); // TODO
		// place
		// controls
		// located
		// in
		// main.xml
		// to
		// SettingsActivity
		// addTab("Dashboard", DashboardFragment.class);
		//
		// setSelectedTab(2);

		// --------------------------
		// --------------------------
		// --------------------------

		manager = getSupportFragmentManager();

		DashboardFragment firstFragment = new DashboardFragment();
		manager.beginTransaction().replace(R.id.content_frame, firstFragment)
				.commit();

		prepareNavDrawerItems();
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setAdapter(new NavAdapter());
		ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
				this, drawer, R.drawable.ic_drawer, R.string.open_drawer,
				R.string.close_drawer) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				prepareNavDrawerItems();
			}
		};

		drawer.setDrawerListener(actionBarDrawerToggle);
		drawerList.setOnItemClickListener(this);

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

	private class NavAdapter extends BaseAdapter {
		

		@Override
		public boolean isEnabled(int position) {
			return navDrawerItems[position].isEnabled();
		}
		
		@Override
		public int getCount() {
			return navDrawerItems.length;
		}

		@Override
		public Object getItem(int arg0) {
			return navDrawerItems[arg0];
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			NavMenuItem currentItem = ((NavMenuItem) getItem(position));
			View item;
			if(currentItem.getSubtitle().equals("")){
				item = View.inflate(MainActivity.this,R.layout.nav_item_1, null);
				
			} else {
				item = View.inflate(MainActivity.this,R.layout.nav_item_2, null);
				TextView textView2 = (TextView) item.findViewById(android.R.id.text2);
				textView2.setText(currentItem.getSubtitle());
			}
			((ImageView) item.findViewById(R.id.nav_item_icon)).setImageResource(R.drawable.content_discard);
			TextView textView = (TextView) item.findViewById(android.R.id.text1);
			textView.setText(currentItem.getTitle());
			TYPEFACE.applyCustomFont((ViewGroup) item, TYPEFACE.Raleway(MainActivity.this));
			return item;
		}

	}
	
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        openFragment(position);
    }

    private void openFragment(int position) {
        FragmentManager manager = getSupportFragmentManager();
        switch (position) {
        case LOGIN:
			Intent loginIntent = new Intent(this, LoginActivity.class);
			startActivity(loginIntent);
            break;
        case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
            break;
        case MY_TRACKS:
            ListMeasurementsFragment listMeasurementFragment = new ListMeasurementsFragment();
            manager.beginTransaction().replace(R.id.content_frame, listMeasurementFragment).commit();
            break;
		case START_UPLOAD:
			((ECApplication) this.getApplicationContext())
					.createNotification("start");
			UploadManager uploadManager = new UploadManager(
					application.getDbAdapterLocal(), getApplication());
			uploadManager.uploadAllTracks();
			break;
		case REMOVE_LOCAL_TRACKS:
			application.getDbAdapterLocal().deleteAllTracks();
			application.setTrack(null);
			Log.i("obd2", "deleted all local tracks");
			break;

		case MENU_GARAGE:
			Intent garageIntent = new Intent(this, MyGarage.class);
			startActivityForResult(garageIntent, REQUEST_MY_GARAGE);
			break;
		case START_STOP_MEASUREMENT:
			if (!application.getServiceConnector().isRunning()) {
				application.startConnection();
			} else {
				application.stopConnection();
			}
			break;

        default:
            break;
        }
        drawer.closeDrawer(drawerList);

    }	

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds navDrawerItems to the action bar if it is present.
//		getSupportMenuInflater().inflate(R.menu.menu, menu);
//		return true;
//	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

		application.closeDb();

		// Remove the services etc.

		application.destroyStuff();

	}


	/**
	 * Determine what the menu buttons do
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			if (drawer.isDrawerOpen(drawerList)) {
				drawer.closeDrawer(drawerList);
			} else {
				drawer.openDrawer(drawerList);
			}
			return true;
		
		case START_STOP_MEASUREMENT:
			if (!application.getServiceConnector().isRunning()) {
				application.startConnection();
			} else {
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
			((ECApplication) this.getApplicationContext())
					.createNotification("start");
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
			startActivityForResult(garageIntent, REQUEST_MY_GARAGE);
			return true;
		}
		return false;
	}

	/**
	 * Activate or deactivate the menu navDrawerItems
	 */
//	public boolean onPrepareOptionsMenu(Menu menu) {
//
//		NavMenuItem startStop = menu.findItem(START_STOP_MEASUREMENT);
//		NavMenuItem settings = menu.findItem(SETTINGS);
//		NavMenuItem upload = menu.findItem(START_UPLOAD);
//		NavMenuItem myGarage = menu.findItem(MENU_GARAGE);
//		NavMenuItem loginRegister = menu.findItem(LOGIN);
//
//		if (application.requirementsFulfilled()) { // was requirementsFulfilled
//			try {
//				if (application.getServiceConnector().isRunning()) {
//					startStop.setTitle(R.string.menu_stop);
//					// stop.setEnabled(true);
//					settings.setEnabled(false);
//				} else {
//					startStop.setTitle(R.string.menu_start);
//
//					// Only enable start button when adapter is selected
//
//					SharedPreferences preferences = PreferenceManager
//							.getDefaultSharedPreferences(this);
//
//					String remoteDevice = preferences.getString(
//							car.io.activity.SettingsActivity.BLUETOOTH_KEY,
//							null);
//
//					if (remoteDevice != null) {
//						startStop.setEnabled(true);
//					} else {
//						startStop.setEnabled(false);
//					}
//					settings.setEnabled(true);
//				}
//			} catch (NullPointerException e) {
//				Log.e("obd2", "The Service Connector is null.");
//				startStop.setEnabled(false);
//				settings.setEnabled(true);
//				e.printStackTrace();
//			}
//		} else {
//			startStop.setTitle(R.string.menu_start);
//			startStop.setEnabled(false);
//			settings.setEnabled(false);
//		}
//
//		if (application.getDbAdapterLocal().getAllTracks().size() > 0
//				&& application.isLoggedIn()) {
//			upload.setEnabled(true);
//		} else {
//			upload.setEnabled(false);
//		}
//
//		if (application.isLoggedIn()) {
//			myGarage.setEnabled(true);
//			loginRegister.setTitle(String.format(
//					getResources().getString(R.string.logged_in_as),
//					application.getUser().getUsername()));
//		} else {
//			myGarage.setEnabled(false);
//		}
//
//		return true;
//	}

	// @Override
	// public void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// switch(requestCode){
	// case REQUEST_MY_GARAGE:
	// ((DashboardFragment) getFragmentByPosition(2)).updateSensorOnDashboard();
	// break;
	// case REQUEST_REDIRECT_TO_GARAGE:
	// if(resultCode == REQUEST_MY_GARAGE){
	// Intent garageIntent = new Intent(this, MyGarage.class);
	// startActivityForResult(garageIntent,REQUEST_MY_GARAGE);
	// }
	// ((DashboardFragment) getFragmentByPosition(2)).updateSensorOnDashboard();
	// break;
	// }
	// }
}
