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

package org.envirocar.app.activity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.NavMenuItem;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.application.service.BackgroundService;
import org.envirocar.app.exception.TracksException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.util.NamedThreadFactory;
import org.envirocar.app.views.TypefaceEC;
import org.envirocar.app.views.Utils;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


/**
 * Main UI application that cares about the auto-upload, auto-connect and global
 * UI elements
 * 
 * @author jakob
 * @author gerald
 * 
 * @param <AndroidAlarmService>
 */
public class MainActivity<AndroidAlarmService> extends SherlockFragmentActivity implements OnItemClickListener {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

	public ECApplication application;

	private FragmentManager manager;
	//Navigation Drawer
	private DrawerLayout drawer;
	private ListView drawerList;
	private NavAdapter navDrawerAdapter;

	// Menu Items
	private NavMenuItem[] navDrawerItems;

	static final int DASHBOARD = 0;
	static final int LOGIN = 1;
	static final int MY_TRACKS = 2;
	static final int START_STOP_MEASUREMENT = 3;
	static final int SETTINGS = 4;
	static final int HELP = 5;
	static final int SEND_LOG = 6;

	public static final int REQUEST_MY_GARAGE = 1336;
	public static final int REQUEST_REDIRECT_TO_GARAGE = 1337;
	
	private static final Logger logger = Logger.getLogger(MainActivity.class);
	
	// Include settings for auto upload and auto-connect
	
	private SharedPreferences preferences = null;
	boolean alwaysUpload = false;
	boolean uploadOnlyInWlan = true;
	private Handler handler_upload;
	private boolean serviceRunning;
	private BroadcastReceiver receiver;
	private OnSharedPreferenceChangeListener settingsReceiver;
		
	private void prepareNavDrawerItems(){
		if(this.navDrawerItems == null){
			navDrawerItems = new NavMenuItem[7];
			navDrawerItems[LOGIN] = new NavMenuItem(LOGIN, getResources().getString(R.string.menu_login),R.drawable.device_access_accounts);
			navDrawerItems[SETTINGS] = new NavMenuItem(SETTINGS, getResources().getString(R.string.menu_settings),R.drawable.action_settings);
			navDrawerItems[START_STOP_MEASUREMENT] = new NavMenuItem(START_STOP_MEASUREMENT, getResources().getString(R.string.menu_start),R.drawable.av_play);
			navDrawerItems[DASHBOARD] = new NavMenuItem(DASHBOARD, getResources().getString(R.string.dashboard), R.drawable.dashboard);
			navDrawerItems[MY_TRACKS] = new NavMenuItem(MY_TRACKS, getResources().getString(R.string.my_tracks),R.drawable.device_access_storage);
			navDrawerItems[HELP] = new NavMenuItem(HELP, getResources().getString(R.string.menu_help), R.drawable.action_help);
			navDrawerItems[SEND_LOG] = new NavMenuItem(SEND_LOG, getResources().getString(R.string.menu_send_log), R.drawable.action_report);
		}
		
		if (UserManager.instance().isLoggedIn()) {
			navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_logout));
			navDrawerItems[LOGIN].setSubtitle(String.format(getResources().getString(R.string.logged_in_as),UserManager.instance().getUser().getUsername()));
		} else {
			navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_login));
			navDrawerItems[LOGIN].setSubtitle("");		
		}

		navDrawerAdapter.notifyDataSetChanged();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main_layout);

		application = ((ECApplication) getApplication());
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		alwaysUpload = preferences.getBoolean(SettingsActivity.ALWAYS_UPLOAD, false);
        uploadOnlyInWlan = preferences.getBoolean(SettingsActivity.WIFI_UPLOAD, true);
        handler_upload = new Handler();

		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("");

		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TypefaceEC.Newscycle(this));
		}

		actionBar.setLogo(getResources().getDrawable(R.drawable.actionbarlogo_with_padding));
		
		manager = getSupportFragmentManager();

		DashboardFragment initialFragment = new DashboardFragment();
		manager.beginTransaction().replace(R.id.content_frame, initialFragment)
				.commit();
		
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		navDrawerAdapter = new NavAdapter();
		prepareNavDrawerItems();
		updateStartStopButton();
		drawerList.setAdapter(navDrawerAdapter);
		ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
				this, drawer, R.drawable.ic_drawer, R.string.open_drawer,
				R.string.close_drawer) {
		
			@Override
			public void onDrawerOpened(View drawerView) {
				prepareNavDrawerItems();
				super.onDrawerOpened(drawerView);
			}
		};

		drawer.setDrawerListener(actionBarDrawerToggle);
		drawerList.setOnItemClickListener(this);
		
		manager.executePendingTransactions();

		receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(BackgroundService.SERVICE_STATE)) {
					serviceRunning = intent.getBooleanExtra(BackgroundService.SERVICE_STATE, false);
					updateStartStopButton();
				}
			}
		};
		registerReceiver(receiver, new IntentFilter(BackgroundService.SERVICE_STATE));

		settingsReceiver = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (key.equals(SettingsActivity.BLUETOOTH_KEY)) {
					updateStartStopButton();
				}
				else if (key.equals(ECApplication.PREF_KEY_SENSOR_ID)) {
					updateStartStopButton();
				}
			}
		};
		preferences.registerOnSharedPreferenceChangeListener(settingsReceiver);
		
		/*
		 * Auto-Uploader of tracks. Uploads complete tracks every 10 minutes.
		 */

		ScheduledExecutorService uploadTaskExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getClass().getName()+"-Factory"));
		uploadTaskExecutor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if (alwaysUpload) {
					logger.info("Automatic upload will start");
					if (UserManager.instance().isLoggedIn()) {
						try {
							if (!serviceRunning) {
								logger.info("Service connector not running");
								if (uploadOnlyInWlan == true) {
									if (mWifi.isConnected()) {
										logger.info("Uploading tracks 1");
										handler_upload.post(new Runnable() {

											@Override
											public void run() {
												uploadTracks();
											}
										});
									}
								} else {
									logger.info("Uploading tracks 2");
									handler_upload.post(new Runnable() {

										@Override
										public void run() {
											uploadTracks();
										}
									});
								}
							}
						} catch (NullPointerException e) {
							logger.warn(e.getMessage(), e);
							if (uploadOnlyInWlan == true) {
								if (mWifi.isConnected()) {
									logger.info("Uploading tracks 3 ");
									handler_upload.post(new Runnable() {

										@Override
										public void run() {
											uploadTracks();
										}
									});
								}
							} else {
								logger.info("Uploading tracks 4");
								handler_upload.post(new Runnable() {

									@Override
									public void run() {
										uploadTracks();
									}
								});
							}
						}
					}
				} else {
					logger.info("automatic upload not wanted by user");
				}
			}
		}, 0, 10, TimeUnit.MINUTES);
		
	}

	protected void updateStartStopButton() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null && adapter.isEnabled()) { // was requirementsFulfilled
			try {
				if (serviceRunning) {
					navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_stop));
					navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.av_pause);
				} else {
					navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));
					// Only enable start button when adapter is selected
	
					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(this);
	
					String remoteDevice = preferences.getString(
							org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,
							null);
	
					if (remoteDevice != null) {
						if(!preferences.contains(ECApplication.PREF_KEY_SENSOR_ID)){
							navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
							navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
							navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.no_sensor_selected));
						} else {
							navDrawerItems[START_STOP_MEASUREMENT].setEnabled(true);
							navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.av_play);
							navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(preferences.getString(SettingsActivity.BLUETOOTH_NAME, ""));
						}
					} else {
						navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
						navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
						navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.pref_summary_chose_adapter));
					}

				}
			} catch (NullPointerException e) {
				logger.warn(e.getMessage(), e);
				navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
				navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
			}
		} else {
			navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));
			navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.pref_bluetooth_disabled));
			navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
			navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
		}
		
		navDrawerAdapter.notifyDataSetChanged();
	}

	/**
	 * Helper method for the automatic upload of local tracks via the scheduler.
	 */
    private void uploadTracks() {
        DbAdapter dbAdapter = application.getDBAdapter();
            try {
                if (dbAdapter.getNumberOfLocalTracks() > 0
                        && dbAdapter.getLastUsedTrack()
                                .getNumberOfMeasurements() > 0) {
                    UploadManager uploadManager = new UploadManager(
                            application.getApplicationContext());
                    uploadManager.uploadAllTracks();
                } else {
                	logger.info("Uploading does not make sense right now");
                }
            } catch (TracksException e) {
            	logger.warn("Upload Failed!", e);
            }
        
    }

	private class NavAdapter extends BaseAdapter {
		

		@Override
		public boolean isEnabled(int position) {
			//to allow things like start bluetooth or go to settings from "disabled" action
			return (position == START_STOP_MEASUREMENT ? true : navDrawerItems[position].isEnabled());
		}
		
		@Override
		public int getCount() {
			return navDrawerItems.length;
		}

		@Override
		public Object getItem(int position) {
			return navDrawerItems[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
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
				if(!currentItem.isEnabled()) textView2.setTextColor(Color.GRAY);
			}
			ImageView icon = ((ImageView) item.findViewById(R.id.nav_item_icon));
			icon.setImageResource(currentItem.getIconRes());
			TextView textView = (TextView) item.findViewById(android.R.id.text1);
			textView.setText(currentItem.getTitle());
			if(!currentItem.isEnabled()){
				textView.setTextColor(Color.GRAY);
				icon.setColorFilter(Color.GRAY);
			}
			TypefaceEC.applyCustomFont((ViewGroup) item, TypefaceEC.Raleway(MainActivity.this));
			return item;
		}

	}
	
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        openFragment(position);
    }

    private void openFragment(int position) {
        FragmentManager manager = getSupportFragmentManager();

        switch (position) {
        
        // Go to the dashboard
        
        case DASHBOARD:
        	DashboardFragment dashboardFragment = new DashboardFragment();
            manager.beginTransaction().replace(R.id.content_frame, dashboardFragment).commit();
            break;
            
        //Start the Login activity
            
        case LOGIN:
        	if(UserManager.instance().isLoggedIn()){
        		UserManager.instance().logOut();
    			ListMeasurementsFragment listMeasurementsFragment = (ListMeasurementsFragment) getSupportFragmentManager().findFragmentByTag("MY_TRACKS");
    			// check if this fragment is initialized
    			if (listMeasurementsFragment != null) {
    				listMeasurementsFragment.clearRemoteTracks();
    			} 
        		Crouton.makeText(this, R.string.bye_bye, Style.CONFIRM).show();
        	} else {
                LoginFragment loginFragment = new LoginFragment();
                manager.beginTransaction().replace(R.id.content_frame, loginFragment, "LOGIN").addToBackStack(null).commit();
        	}
            break;
            
        // Go to the settings
            
        case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
            break;
            
        // Go to the track list
            
        case MY_TRACKS:
            ListMeasurementsFragment listMeasurementFragment = new ListMeasurementsFragment();
            manager.beginTransaction().replace(R.id.content_frame, listMeasurementFragment, "MY_TRACKS").addToBackStack(null).commit();
            break;
            
        // Start or stop the measurement process
            
		case START_STOP_MEASUREMENT:
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

			String remoteDevice = preferences.getString(org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,null);

			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter != null && adapter.isEnabled() && remoteDevice != null) {
				if(!preferences.contains(ECApplication.PREF_KEY_SENSOR_ID)){
			        MyGarage garageFragment = new MyGarage();
			        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, garageFragment).addToBackStack(null).commit();
				} else {
					if (!serviceRunning) {
						application.startConnection();
						Crouton.makeText(this, R.string.start_measuring, Style.INFO).show();
					} else {
						application.stopConnection();
						Crouton.makeText(this, R.string.stop_measuring, Style.INFO).show();
					}
				}
			} else {
				Intent settingsIntent = new Intent(this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
			break;
		case HELP:
			HelpFragment helpFragment = new HelpFragment();
            manager.beginTransaction().replace(R.id.content_frame, helpFragment, "HELP").addToBackStack(null).commit();
			break;
		case SEND_LOG:
			SendLogFileFragment logFragment = new SendLogFileFragment();
			manager.beginTransaction().replace(R.id.content_frame, logFragment, "SEND_LOG").addToBackStack(null).commit();
        default:
            break;
        }
        drawer.closeDrawer(drawerList);

    }

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

//		application.closeDb();

		// Remove the services etc.

//		application.destroyStuff();
		
		Crouton.cancelAllCroutons();
		
//		this.unregisterReceiver(application.getBluetoothChangeReceiver());

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		drawer.closeDrawer(drawerList);
	    //first init
	    firstInit();
	    
	    application.setActivity(this);
	    
		if (preferences.getBoolean(SettingsActivity.DISPLAY_STAYS_ACTIV, false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	    
		alwaysUpload = preferences.getBoolean(SettingsActivity.ALWAYS_UPLOAD, false);
        uploadOnlyInWlan = preferences.getBoolean(SettingsActivity.WIFI_UPLOAD, true);
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
		}
		return false;
	}
	
	private void firstInit(){
		if(!preferences.contains("first_init")){
			drawer.openDrawer(drawerList);
			
			Editor e = preferences.edit();
			e.putString("first_init", "seen");
			e.putBoolean("pref_privacy", true);
			e.commit();
		}
	}

}
