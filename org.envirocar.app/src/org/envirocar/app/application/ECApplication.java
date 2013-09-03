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

package org.envirocar.app.application;

import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.envirocar.app.R;
import org.envirocar.app.activity.MainActivity;
import org.envirocar.app.application.service.BackgroundService;
import org.envirocar.app.application.service.BackgroundServiceConnector;
import org.envirocar.app.application.service.BackgroundServiceInteractor;
import org.envirocar.app.application.service.DeviceInRangeService;
import org.envirocar.app.logging.ACRACustomSender;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * This is the main application that is the central linking component for all adapters, services and so on.
 * This application is implemented like a singleton, it exists only once while the app is running.
 * @author gerald, jakob
 *
 */
@ReportsCrashes(formKey = "")
public class ECApplication extends Application {
	
	private static final Logger logger = Logger.getLogger(ECApplication.class);
	
	// Strings

	public static final String BASE_URL = "https://giv-car.uni-muenster.de/stable/rest";

	public static final String PREF_KEY_CAR_MODEL = "carmodel";
	public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
	public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
	public static final String PREF_KEY_FUEL_TYPE = "fueltype";
	public static final String PREF_KEY_SENSOR_ID = "sensorid";
	public static final String PREF_KEY_CAR_ENGINE_DISPLACEMENT = "pref_engine_displacement";

	private SharedPreferences preferences = null;
	
	// Helpers and objects

//	private DbAdapter dbAdapterLocal;
//	private DbAdapter dbAdapterRemote;
	private DbAdapter dbAdapter;
	private final ScheduledExecutorService scheduleTaskExecutor = Executors
			.newScheduledThreadPool(1);
	private BluetoothAdapter bluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	private BackgroundServiceConnector serviceConnector;
	private Intent backgroundService;
	private Intent deviceInRangeService;
	
	private Listener commandListener;
	
	private int mId = 1133;
	
	protected boolean adapterConnected;
	private Activity currentActivity;
	
	private final BroadcastReceiver bluetoothChangeReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();

	        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
	                                                 BluetoothAdapter.ERROR);
	            switch (state) {
	            case BluetoothAdapter.STATE_ON:
	            	logger.info("bt is now on");
	            	initializeBackgroundServices();
	                break;
	            }
	        }
	        else if (action.equals(DeviceInRangeService.DEVICE_FOUND)) {
	        	logger.info("our device got discovered!");
	        	startConnection();
	        }
	    }
	};

	private BroadcastReceiver receiver;
	

	/**
	 * returns the current activity.
	 * @return
	 */
	public Activity getCurrentActivity(){
		return currentActivity;
	}
	

	public void setActivity(Activity a){
		this.currentActivity = a;
	}
	
	/**
	 * Returns the service connector of the server
	 * @return the serviceConnector
	 */
	public BackgroundServiceConnector getServiceConnector() {
		if (serviceConnector == null)
			initializeBackgroundServices();
		return serviceConnector;
	}
	
	/**
	 * Returns whether requirements were fulfilled (bluetooth activated)
	 * @return requirementsFulfilled?
	 */
	public boolean bluetoothActivated() {
		if (bluetoothAdapter == null) {
			logger.warn("Bluetooth disabled");
			return false;
		} else {
			logger.info("Bluetooth enabled");
			return bluetoothAdapter.isEnabled();
		}
	}


	@Override
	public void onCreate() {
		Logger.initialize(getVersionString());
		super.onCreate();
		
		initializeErrorHandling();
		
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		UserManager.init(getApplicationContext());
		initDbAdapter();
		// Make a new commandListener to interpret the measurement values that are
		// returned
		logger.info("init commandListener");
		createListeners();
		
		// If everything is available, start the service connector and commandListener
		initializeBackgroundServices();
		
		//bluetooth change commandListener
	    registerReceiver(bluetoothChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	    registerReceiver(bluetoothChangeReceiver, new IntentFilter(DeviceInRangeService.DEVICE_FOUND));

	}
	
//	private void testBluetooth() {
//	    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//		
//		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//		// If there are paired devices
//		BluetoothDevice theDevice = null;
//		if (pairedDevices.size() > 0) {
//		    // Loop through paired devices
//		    for (BluetoothDevice device : pairedDevices) {
//		        // Add the name and address to an array adapter to show in a ListView
//		    	if (device.getName().contains("20") || device.getName().contains("_W_W_")) {
//		    		theDevice = device;
//		    	}
//		    }
//		}
//		
//		
//
//	}

	

	
	
	private void initializeErrorHandling() {
		ACRA.init(this);
		ACRACustomSender yourSender = new ACRACustomSender();
		ACRA.getErrorReporter().setReportSender(yourSender);
	}

	/**
	 * This method opens both dbadapters or also gets them and opens them afterwards.
	 */
	private void initDbAdapter() {
		if (dbAdapter == null) {
			dbAdapter = new DbAdapterImpl(this.getApplicationContext());
			dbAdapter.open();
		}
	}

	public DbAdapter getDBAdapter() {
		initDbAdapter();
		return dbAdapter;
	}


	/**
	 * This method starts the service that connects to the adapter to the app.
	 */
	private void initializeBackgroundServices() {
		if (bluetoothActivated()) {
			logger.info("requirements met");
			deviceInRangeService = new Intent(this, DeviceInRangeService.class);
			startService(deviceInRangeService);
			backgroundService = new Intent(this, BackgroundService.class);
			serviceConnector = new BackgroundServiceConnector(commandListener);
			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
			
			receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(BackgroundService.CONNECTION_NOT_YET_VERIFIED_INTENT)) {
						serviceConnector.executeInitializationSequence();
					}
					else if (intent.getAction().equals(BackgroundService.CONNECTION_VERIFIED_INTENT)) {
						onAdapterConnected();
					}
					else if (intent.getAction().equals(BackgroundService.DISCONNECTED_INTENT)) {
						onAdapterDisconnected();
					}
					else if (intent.getAction().equals(BackgroundServiceInteractor.CONNECTION_PERMANENTLY_FAILED_INTENT)) {
						connectionPermanentlyFailed();
					}
				}
			};
			
			registerReceiver(bluetoothChangeReceiver, new IntentFilter(BackgroundService.CONNECTION_NOT_YET_VERIFIED_INTENT));
			registerReceiver(receiver, new IntentFilter(BackgroundService.CONNECTION_VERIFIED_INTENT));
			registerReceiver(receiver, new IntentFilter(BackgroundService.DISCONNECTED_INTENT));
			registerReceiver(receiver, new IntentFilter(BackgroundServiceInteractor.CONNECTION_PERMANENTLY_FAILED_INTENT));
		} else {
			logger.warn("bluetooth not activated!");
		}
	}

	
	/**
	 * This method starts the service connector every five minutes if the user 
	 * wants an autoconnection
	 * @deprecated not being used. implement a better way at a better location if required
	 */
	@Deprecated
	public void startServiceConnector() {
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (bluetoothActivated()) {
					if (!serviceConnector.isRunning()) {
						startConnection();
					} else {
						logger.warn("serviceConnector not running");
					}
				} else {
					logger.warn("requirementsFulfilled was false!");
				}

			}
		}, 0, 5, TimeUnit.MINUTES);
	}

	/**
	 * This method starts the commandListener that interprets the answers from the BT adapter.
	 */
	public void createListeners() {
		//TODO de-couple dbAdapterLocal
		commandListener = new CommandListener(createCar(), dbAdapter);
	}

	private Car createCar() {
		String fuelType = preferences
				.getString(PREF_KEY_FUEL_TYPE, "undefined");
		String carManufacturer = preferences.getString(
				PREF_KEY_CAR_MANUFACTURER, "undefined");
		String carModel = preferences
				.getString(PREF_KEY_CAR_MODEL, "undefined");
		String sensorId = preferences
				.getString(PREF_KEY_SENSOR_ID, "undefined");
		String displacement = preferences.getString(PREF_KEY_CAR_ENGINE_DISPLACEMENT,"2.0");
		FuelType type = null;
		if (fuelType.equalsIgnoreCase(FuelType.GASOLINE.toString())) {
			type = FuelType.GASOLINE;
		} else {
			type = FuelType.DIESEL;
		}
		Car car = new Car(type, carManufacturer, carModel, sensorId, Double.parseDouble(displacement));
		return car;
	}

	/**
	 * Stop the service connector and therefore the scheduled tasks.
	 */
	public void shutdownServiceConnector() {
		scheduleTaskExecutor.shutdown();
	}

	/**
	 * Connects to the Bluetooth Adapter and starts the execution of the
	 * commands. also opens the db and starts the gps.
	 */
	public void startConnection() {
		logger.info("Starts the recording of a track");
		initDbAdapter();
		//createNewTrackIfNecessary();
		if (!serviceConnector.isRunning()) {
			startService(backgroundService);
		}

	}

	/**
	 * Ends the connection with the Bluetooth Adapter. also stops gps and closes the db.
	 */
	public void stopConnection() {
		logger.info("Stops the recording of a track");
		if (serviceConnector != null && serviceConnector.isRunning()) {
			stopService(backgroundService);
			serviceConnector.shutdownBackgroundService();
		}
		closeDb();
	}


	/**
	 * Stops gps, kills service, kills service connector, kills commandListener and handler
	 */
	public void destroyStuff() {
		backgroundService = null;
		serviceConnector = null;
	}

	/**
	 * Closes both databases.
	 */
	public void closeDb() {
		if (dbAdapter != null) {
			dbAdapter.close();
		}
	}
	
	/**
	 * 
	 * @action Can also contain the http status code with error if fail
	 */
	public void createNotification(String action) {
		String notification_text = "";
		if(action.equals("success")){
			notification_text = getString(R.string.upload_notification_success);
		}else if(action.equals("start")){
			notification_text = getString(R.string.upload_notification);
		}else{
			notification_text = action;
		}
		
		Intent intent = new Intent(this,MainActivity.class);
		PendingIntent pintent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("EnviroCar")
		        .setContentText(notification_text)
		        .setContentIntent(pintent)
		        .setTicker(notification_text)
		        .setProgress(0, 0, !action.equals("success"));
		
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(mId, mBuilder.build());

	  }
	
	/**
	 * method to get the current version
	 * 
	 */
	public String getVersionString() {
		StringBuilder out = new StringBuilder("Version ");
		try {
			out.append(this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			out.append(" (");
			out.append(this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
			out.append("), ");
		} catch (NameNotFoundException e) {
			logger.warn(e.getMessage(), e);
		}
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(
					getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			out.append(SimpleDateFormat.getInstance().format(new java.util.Date(time)));

		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		return out.toString();
	}

	
	public void resetTrack() {
		this.commandListener.resetTrack();
	}

	private void onAdapterConnected() {
		displayToast("OBD-II Adapter connected");
	}

	private void onAdapterDisconnected() {
		displayToast("OBD-II Adapter disconnected");	
	}

	private void connectionPermanentlyFailed() {
		displayToast("OBD-II Adapter connection permanently failed");
	}

	private void displayToast(final String string) {
		if (getCurrentActivity() == null) return;
		
		getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG).show();				
			}
		});		
	}


}
