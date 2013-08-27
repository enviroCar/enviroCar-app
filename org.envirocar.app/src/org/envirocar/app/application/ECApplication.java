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
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.logging.ACRACustomSender;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterLocal;
import org.envirocar.app.storage.DbAdapterRemote;
import org.envirocar.app.util.AndroidUtil;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

/**
 * This is the main application that is the central linking component for all adapters, services and so on.
 * This application is implemented like a singleton, it exists only once while the app is running.
 * @author gerald, jakob
 *
 */
@ReportsCrashes(formKey = "")
public class ECApplication extends Application implements LocationListener {
	
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

	private DbAdapter dbAdapterLocal;
	private DbAdapter dbAdapterRemote;
	private final ScheduledExecutorService scheduleTaskExecutor = Executors
			.newScheduledThreadPool(1);
	private BluetoothAdapter bluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	private ServiceConnector serviceConnector = null;
	private Intent backgroundService = null;
	private Handler handler = new Handler();
	private Listener listener = null;
	private LocationManager locationManager;
	private int mId = 1133;
	
	
	private Activity currentActivity;
	
	//private boolean requirementsFulfilled = true;

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
	public ServiceConnector getServiceConnector() {
		if (serviceConnector == null)
			startBackgroundService();
		return serviceConnector;
	}
	
	/**
	 * Returns whether requirements were fulfilled (bluetooth activated)
	 * @return requirementsFulfilled?
	 */
	public boolean requirementsFulfilled() {
		if (bluetoothAdapter == null) {
			logger.warn("Bluetooth disabled");
			return false;
		} else {
			logger.warn("Bluetooth enabled");
			return bluetoothAdapter.isEnabled();
		}
	}

	/**
	 * This method updates the attributes of the current sensor (=car) 
	 * @param sensorid the id that is stored on the server
	 * @param carManufacturer the car manufacturer
	 * @param carModel the car model
	 * @param fuelType the fuel type of the car
	 * @param year construction year of the car
	 */
	public void updateCurrentSensor(String sensorid, String carManufacturer,
			String carModel, String fuelType, int year) {
		Editor e = preferences.edit();
		e.putString(PREF_KEY_SENSOR_ID, sensorid);
		e.putString(PREF_KEY_CAR_MANUFACTURER, carManufacturer);
		e.putString(PREF_KEY_CAR_MODEL, carModel);
		e.putString(PREF_KEY_FUEL_TYPE, fuelType);
		e.putString(PREF_KEY_CAR_CONSTRUCTION_YEAR, year + "");
		e.commit();
	}

	@Override
	public void onCreate() {
		Logger.initialize(getVersionString());
		super.onCreate();
		
		initializeErrorHandling();
		
		AndroidUtil.init(getApplicationContext());
		
		preferences = AndroidUtil.getInstance().getDefaultSharedPreferences();

		UserManager.init(getApplicationContext());
		initDbAdapter();
		initLocationManager();
		startLocationManager();
		// Make a new listener to interpret the measurement values that are
		// returned
		logger.info("init listener");
		startListener();
		// If everything is available, start the service connector and listener
		startBackgroundService();
	}
	
	private void initializeErrorHandling() {
		ACRA.init(this);
		ACRACustomSender yourSender = new ACRACustomSender();
		ACRA.getErrorReporter().setReportSender(yourSender);
	}


	/**
	 * This method opens both dbadapters or also gets them and opens them afterwards.
	 */
	private void initDbAdapter() {
		if (dbAdapterLocal == null) {
			dbAdapterLocal = new DbAdapterLocal(this.getApplicationContext());
			dbAdapterLocal.open();
		} else {
			if (!dbAdapterLocal.isOpen())
				dbAdapterLocal.open();
		}
		if (dbAdapterRemote == null) {
			dbAdapterRemote = new DbAdapterRemote(this.getApplicationContext());
			dbAdapterRemote.open();
		} else {
			if (!dbAdapterRemote.isOpen())
				dbAdapterRemote.open();
		}
	}

	/**
	 * Returns the local db adadpter. This has to be called by other
	 * functions in order to work with the data (change tracks and measurements).
	 * @return the local db adapter
	 */
	public DbAdapter getDbAdapterLocal() {
		initDbAdapter();
		return dbAdapterLocal;
	}

	/**
	 * Get the remote db adapter (to work with the measurements from the server).
	 * @return the remote dbadapter
	 */
	public DbAdapter getDbAdapterRemote() {
		initDbAdapter();
		return dbAdapterRemote;
	}

	/**
	 * Starts the location manager again after an resume.
	 */
	public void startLocationManager() {
		if(locationManager == null){
			initLocationManager();
		}
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, this);
	}

	/**
	 * Stops the location manager (removeUpdates) for pause.
	 */
	public void stopLocating() {
		if(locationManager != null){
			locationManager.removeUpdates(this);
		}
	}

	/**
	 * This method starts the service that connects to the adapter to the app.
	 */
	public void startBackgroundService() {
		if (requirementsFulfilled()) {
			logger.info("requirements met");
			backgroundService = new Intent(this, BackgroundService.class);
			serviceConnector = new ServiceConnector();
			serviceConnector.setServiceListener(listener);

			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		} else {
			logger.warn("requirements not met");
		}
	}

	/**
	 * This method starts the service connector every five minutes if the user 
	 * wants an autoconnection
	 */
	public void startServiceConnector() {
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (requirementsFulfilled()) {
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
	 * This method starts the listener that interprets the answers from the BT adapter.
	 */
	public void startListener() {
		//TODO de-couple dbAdapterLocal
		listener = new CommandListener(createCar(), dbAdapterLocal);
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
	public void stopServiceConnector() {
		scheduleTaskExecutor.shutdown();
	}

	/**
	 * Connects to the Bluetooth Adapter and starts the execution of the
	 * commands. also opens the db and starts the gps.
	 */
	public void startConnection() {
		logger.info("Starts the recording of a track");
		initDbAdapter();
		startLocationManager();
		//createNewTrackIfNecessary();
		if (!serviceConnector.isRunning()) {
			startService(backgroundService);
			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		}
		try {
			handler.post(waitingListRunnable);
		} catch (Exception e) {
			logger.severe("NullPointerException occured: Handler is null: " + (handler == null) + " waitingList is null: " + (waitingListRunnable == null), e);
		}
	}

	/**
	 * Ends the connection with the Bluetooth Adapter. also stops gps and closes the db.
	 */
	public void stopConnection() {
		logger.info("Stops the recording of a track");
		if (serviceConnector != null && serviceConnector.isRunning()) {
			stopService(backgroundService);
			unbindService(serviceConnector);
		}
		handler.removeCallbacks(waitingListRunnable);

		stopLocating();
		closeDb();
	}

	/**
	 * Handles the waiting-list
	 */
	private Runnable waitingListRunnable = new Runnable() {
		public void run() {

			if (serviceConnector != null && serviceConnector.isRunning())
				addCommandstoWaitinglist();

			try {
				handler.postDelayed(waitingListRunnable, 2000);
			} catch (NullPointerException e) {
				logger.severe("NullPointerException occured: Handler is null: " + (handler == null) + " waitingList is null: " + (waitingListRunnable == null), e);
			}
		}
	};

	/**
	 * Helper method that adds the desired commands to the waiting list where
	 * all commands are executed
	 */
	private void addCommandstoWaitinglist() {
		final CommonCommand speed = new Speed();
		final CommonCommand maf = new MAF();
		final CommonCommand rpm = new RPM();
		final CommonCommand intakePressure = new IntakePressure();
		final CommonCommand intakeTemperature = new IntakeTemperature();
		
		serviceConnector.addJobToWaitingList(speed);
		serviceConnector.addJobToWaitingList(maf);
		serviceConnector.addJobToWaitingList(rpm);
		serviceConnector.addJobToWaitingList(intakePressure);
		serviceConnector.addJobToWaitingList(intakeTemperature);
	}



	/**
	 * Init the location Manager
	 */
	private void initLocationManager() {

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		// 0,
		// 0, this);

	}

	/**
	 * Stops gps, kills service, kills service connector, kills listener and handler
	 */
	public void destroyStuff() {
		stopLocating();
		locationManager = null;
		backgroundService = null;
		serviceConnector = null;
//		listener = null;
		handler = null;
	}

	/**
	 * updates the location variables when the device moved
	 */
	@Override
	public void onLocationChanged(Location location) {
		EventBus.getInstance().fireEvent(new LocationEvent(location));
		logger.info("Get new position of " + location.getProvider() + " : lat " + location.getLatitude() + " long: " + location.getLongitude());
	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String arg0) {

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

	}

	/**
	 * Closes both databases.
	 */
	public void closeDb() {
		if (dbAdapterLocal != null) {
			dbAdapterLocal.close();
			// dbAdapterLocal = null;
		}
		if (dbAdapterRemote != null) {
			dbAdapterRemote.close();
			// dbAdapterRemote = null;
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

	
	public final BroadcastReceiver bluetoothChangeReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();

	        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
	                                                 BluetoothAdapter.ERROR);
	            switch (state) {
	            case BluetoothAdapter.STATE_ON:
	            	logger.info("bt is now on");
	            	startBackgroundService();
	                break;
	            }
	        }
	    }
	};

	public void resetTrack() {
		this.listener.resetTrack();
	}


}
