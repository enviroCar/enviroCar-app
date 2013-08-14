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

import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.exception.TracksException;
import org.envirocar.app.logging.ACRACustomSender;
import org.envirocar.app.logging.LocalFileHandler;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterLocal;
import org.envirocar.app.storage.DbAdapterRemote;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.Utils;

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
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
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
public class ECApplication extends Application implements LocationListener {
	
	private static final Logger logger = Logger.getLogger(ECApplication.class);
	
	// Strings

	public static final String BASE_URL = "https://giv-car.uni-muenster.de/stable/rest";

	public static final String PREF_KEY_CAR_MODEL = "carmodel";
	public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
	public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
	public static final String PREF_KEY_FUEL_TYPE = "fueltype";
	public static final String PREF_KEY_SENSOR_ID = "sensorid";

	private SharedPreferences preferences = null;
	private boolean imperialUnits = false;
	
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
	
	// Measurement values
	
	private float locationLatitude;
	private float locationLongitude;
	private int speedMeasurement = 0;
	private double co2Measurement = 0.0;
	private double mafMeasurement;
	private double calculatedMafMeasurement = 0;
	private int rpmMeasurement = 0;
	private int intakeTemperatureMeasurement = 0;
	private int intakePressureMeasurement = 0;
	private Measurement measurement = null;
	private long lastInsertTime = 0;
	
	private Activity currentActivity;
	
	// Track properties

	private Track track;
	private String trackDescription = "Description of the track";

	//private boolean requirementsFulfilled = true;

	private static User user;
	
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
		try {
			Logger.initialize(openFileOutput(LocalFileHandler.LOCAL_LOG_FILE, MODE_APPEND),
					getVersionString());
		} catch (FileNotFoundException e) {
			logger.warn(e.getMessage(), e);
		}
		super.onCreate();
		
		initializeErrorHandling();
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		user = getUserFromSharedPreferences();

		initDbAdapter();
		initLocationManager();
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
	 * Returns the sensor properties as a string
	 * @return
	 */
	public String getCurrentSensorString(){
		if(PreferenceManager.getDefaultSharedPreferences(this).contains(ECApplication.PREF_KEY_SENSOR_ID) && 
				PreferenceManager.getDefaultSharedPreferences(this).contains(ECApplication.PREF_KEY_FUEL_TYPE) &&
				PreferenceManager.getDefaultSharedPreferences(this).contains(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR) &&
				PreferenceManager.getDefaultSharedPreferences(this).contains(ECApplication.PREF_KEY_CAR_MODEL) &&
				PreferenceManager.getDefaultSharedPreferences(this).contains(ECApplication.PREF_KEY_CAR_MANUFACTURER)){
			String prefSensorid = PreferenceManager.getDefaultSharedPreferences(this).getString(ECApplication.PREF_KEY_SENSOR_ID, "nosensor");
			String prefFuelType = PreferenceManager.getDefaultSharedPreferences(this).getString(ECApplication.PREF_KEY_FUEL_TYPE, "nosensor");
			String prefYear = PreferenceManager.getDefaultSharedPreferences(this).getString(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR, "nosensor");
			String prefModel = PreferenceManager.getDefaultSharedPreferences(this).getString(ECApplication.PREF_KEY_CAR_MODEL, "nosensor");
			String prefManu = PreferenceManager.getDefaultSharedPreferences(this).getString(ECApplication.PREF_KEY_CAR_MANUFACTURER, "nosensor");
			if(prefSensorid.equals("nosensor") == false ||
					prefYear.equals("nosensor") == false ||
					prefFuelType.equals("nosensor") == false ||
					prefModel.equals("nosensor") == false ||
					prefManu.equals("nosensor") == false ){
				return prefManu+" "+prefModel+" ("+prefFuelType+" "+prefYear+")";
			}
		}
			return getResources().getString(R.string.no_sensor_selected);
		
	}

	/**
	 * This method determines whether it is necessary to create a new track or
	 * of the current/last used track should be reused
	 */
	public void createNewTrackIfNecessary() {

		// setting undefined, will hopefully prevent correct uploading.
		// but this shouldn't be possible to record tracks without these values
		String fuelType = preferences
				.getString(PREF_KEY_FUEL_TYPE, "undefined");
		String carManufacturer = preferences.getString(
				PREF_KEY_CAR_MANUFACTURER, "undefined");
		String carModel = preferences
				.getString(PREF_KEY_CAR_MODEL, "undefined");
		String sensorId = preferences
				.getString(PREF_KEY_SENSOR_ID, "undefined");

		// if track is null, create a new one or take the last one from the
		// database
		
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH)+1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		String date = String.valueOf(year) + "-" + String.valueOf(month) + "-" + String.valueOf(day);

		if (track == null) {

			logger.info("The track was null");

			Track lastUsedTrack;

			try {
				lastUsedTrack = dbAdapterLocal.getLastUsedTrack();

				try {

					// New track if last measurement is more than 60 minutes
					// ago

					if ((System.currentTimeMillis() - lastUsedTrack
							.getLastMeasurement().getMeasurementTime()) > 3600000) {
						logger.info("I create a new track because the last measurement is more than 60 mins ago");
						track = new Track("123456", fuelType, carManufacturer,
								carModel, sensorId, dbAdapterLocal);
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						track.commitTrackToDatabase();
						return;
					}

					// new track if last position is significantly different
					// from the current position (more than 3 km)
					if (Utils.getDistance(lastUsedTrack.getLastMeasurement().getLatitude(),lastUsedTrack.getLastMeasurement().getLongitude(),
							locationLatitude, locationLongitude) > 3.0) {
						logger.info("The last measurement's position is more than 3 km away. I will create a new track");
						track = new Track("123456", fuelType, carManufacturer,
								carModel, sensorId, dbAdapterLocal); 
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						track.commitTrackToDatabase();
						return;

					}

					// TODO: New track if user clicks on create new track button

					// TODO: new track if VIN changed

					else {
						logger.info("I will append to the last track because that still makes sense");
						track = lastUsedTrack;
						return;
					}

				} catch (MeasurementsException e) {
					logger.warn("The last track contains no measurements. I will delete it and create a new one.");
					dbAdapterLocal.deleteTrack(lastUsedTrack.getId());
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
				}

			} catch (TracksException e) {
				logger.warn("There was no track in the database so I created a new one");
				track = new Track("123456", fuelType, carManufacturer,
						carModel, sensorId, dbAdapterLocal); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				track.commitTrackToDatabase();
			}

			return;

		}

		// if track is not null, determine whether it is useful to create a new
		// track and store the current one

		if (track != null) {

			logger.info("the track was not null");

			Track currentTrack = track;

			try {

				// New track if last measurement is more than 60 minutes
				// ago
				if ((System.currentTimeMillis() - currentTrack
						.getLastMeasurement().getMeasurementTime()) > 3600000) {
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal);
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
					logger.info("I create a new track because the last measurement is more than 60 mins ago");
					return;
				}
				// TODO: New track if user clicks on create new track button

				// new track if last position is significantly different from
				// the
				// current position (more than 3 km)

				if (Utils.getDistance(currentTrack.getLastMeasurement().getLatitude(),currentTrack.getLastMeasurement().getLongitude(),
						locationLatitude, locationLongitude) > 3.0) {
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
					logger.info("The last measurement's position is more than 3 km away. I will create a new track");
					return;

				}

				// TODO: new track if VIN changed

				else {
					logger.info("I will append to the last track because that still makes sense");
					return;
				}

			} catch (MeasurementsException e) {
				logger.warn("The last track contains no measurements. I will delete it and create a new one.");
				dbAdapterLocal.deleteTrack(currentTrack.getId());
				track = new Track("123456", fuelType, carManufacturer,
						carModel, sensorId, dbAdapterLocal); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				track.commitTrackToDatabase();
			}

		}

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
	 * Get a user object from the shared preferences
	 * @return the user that is stored on the device
	 */
	private User getUserFromSharedPreferences() {
		if (preferences.contains("username") && preferences.contains("token")) {
			return new User(preferences.getString("username", "anonymous"),
					preferences.getString("token", "anon"));
		}
		return null;
	}

	/**
	 * Set the user (to the application and also store it in the preferences)
	 * @param user The user you want to set
	 */
	public void setUser(User user) {
		ECApplication.user = user;
		Editor e = preferences.edit();
		e.putString("username", user.getUsername());
		e.putString("token", user.getToken());
		e.apply();
	}

	/**
	 * Get the user
	 * @return user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Determines whether the user is logged in. A user is logged in when
	 * the application has a user as a variable.
	 * @return
	 */
	public boolean isLoggedIn() {
		return user != null;
	}

	/**
	 * Logs out the user.
	 */
	public void logOut() {
		Editor e = preferences.edit();
		if (preferences.contains("username"))
			e.remove("username");
		if (preferences.contains("token"))
			e.remove("token");
		e.commit();
		user = null;
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
		listener = new Listener() {

			public void receiveUpdate(CommonCommand job) {
				logger.debug("update received");
				// Get the name and the result of the Command

				String commandName = job.getCommandName();
				String commandResult = job.getResult();
				logger.debug(commandName + " " + commandResult);
				if (commandResult.equals("NODATA"))
					return;

				/*
				 * Check which measurent is returned and save the value in the
				 * previously created measurement
				 */

				// Speed

				if (commandName.equals("Vehicle Speed")) {

					try {
						speedMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						logger.warn("speed parse exception", e);
					}
				}
				
				//RPM
				
				if (commandName.equals("Engine RPM")) {
					// TextView speedTextView = (TextView)
					// findViewById(R.id.spd_text);
					// speedTextView.setText(commandResult + " km/h");

					try {
						rpmMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						logger.warn("rpm parse exception", e);
					}
				}

				//IntakePressure
				
				if (commandName.equals("Intake Manifold Pressure")) {
					// TextView speedTextView = (TextView)
					// findViewById(R.id.spd_text);
					// speedTextView.setText(commandResult + " km/h");

					try {
						intakePressureMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						logger.warn("Intake Pressure parse exception", e);
					}
				}
				
				//IntakeTemperature
				
				if (commandName.equals("Air Intake Temperature")) {
					// TextView speedTextView = (TextView)
					// findViewById(R.id.spd_text);
					// speedTextView.setText(commandResult + " km/h");

					try {
						intakeTemperatureMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						logger.warn("Intake Temperature parse exception", e);
					}
				}
								
				//calculate alternative maf from iat, map, rpm
				double imap = rpmMeasurement * intakePressureMeasurement / (intakeTemperatureMeasurement+273);
				//VE = 85 in most modern cars
				double calculatedMaf = imap / 120.0d * 85.0d/100.0d * Float.parseFloat(preferences.getString("pref_engine_displacement","2.0")) * 28.97 / 8.317;	
				calculatedMafMeasurement = calculatedMaf;
				
				logger.info("calculatedMaf: "+calculatedMaf+" "+Float.parseFloat(preferences.getString("pref_engine_displacement","2.0"))+" "+preferences.getString("pref_engine_displacement","2.0"));
				
				// MAF

				if (commandName.equals("Mass Air Flow")) {
					String maf = commandResult;

					try {
						NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
						Number number;
						number = format.parse(maf);
						mafMeasurement = number.doubleValue();

						// Dashboard Co2 current value preparation

						double consumption = 0.0;

						if (mafMeasurement > 0.0) {
							if (preferences.getString(PREF_KEY_FUEL_TYPE,
									"gasoline").equals("gasoline")) {
								consumption = (mafMeasurement / 14.7) / 747;
								// Change to l/h
								consumption=consumption*3600;
							} else if (preferences.getString(
									PREF_KEY_FUEL_TYPE, "gasoline").equals(
									"diesel")) {
								consumption = (mafMeasurement / 14.5) / 832;
								// Change to l/h
								consumption=consumption*3600;
							}
						} else {
							if (preferences.getString(PREF_KEY_FUEL_TYPE,
									"gasoline").equals("gasoline")) {
								consumption = (calculatedMafMeasurement / 14.7) / 747;
								// Change to l/h
								consumption=consumption*3600;
							} else if (preferences.getString(
									PREF_KEY_FUEL_TYPE, "gasoline").equals(
									"diesel")) {
								consumption = (calculatedMafMeasurement / 14.5) / 832;
								// Change to l/h
								consumption=consumption*3600;
							}
						}

						if (preferences.getString(PREF_KEY_FUEL_TYPE,
								"gasoline").equals("gasoline")) {
							co2Measurement = consumption * 2.35; //kg/h
						} else if (preferences.getString(PREF_KEY_FUEL_TYPE,
								"gasoline").equals("diesel")) {
							co2Measurement = consumption * 2.65; //kg/h
						}
						logger.info("co2: "+ co2Measurement+"");
					} catch (ParseException e) {
						logger.warn("parse exception maf", e);
					} catch (java.text.ParseException e) {
						logger.warn("parse exception maf", e);
					}
				}

				// Update and insert the measurement

				updateMeasurement();
			}

		};
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
		initDbAdapter();
		startLocationManager();
		//createNewTrackIfNecessary();
		if (!serviceConnector.isRunning()) {
			logger.info("Background service starts");
			startService(backgroundService);
			bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		}
		handler.post(waitingListRunnable);
	}

	/**
	 * Ends the connection with the Bluetooth Adapter. also stops gps and closes the db.
	 */
	public void stopConnection() {

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

			handler.postDelayed(waitingListRunnable, 2000);
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
	 * Helper Command that updates the current measurement with the last
	 * measurement data and inserts it into the database if the measurements is
	 * young enough
	 */
	public void updateMeasurement() {

		// Create track new measurement if necessary

		if (measurement == null) {
			measurement = new Measurement(locationLatitude, locationLongitude);
		}

		// Insert the values if the measurement (with the coordinates) is young
		// enough (5000ms) or create new one if it is too old

		if (measurement.getLatitude() != 0.0 && measurement.getLongitude() != 0.0) {

			if (Math.abs(measurement.getMeasurementTime() - System.currentTimeMillis()) > 5000) {
				measurement = new Measurement(locationLatitude, locationLongitude);
			}

			measurement.setSpeed(speedMeasurement);
			measurement.setMaf(mafMeasurement);	
			measurement.setCalculatedMaf(calculatedMafMeasurement);
			measurement.setRpm(rpmMeasurement);
			measurement.setIntakePressure(intakePressureMeasurement);
			measurement.setIntakeTemperature(intakeTemperatureMeasurement);
			insertMeasurement(measurement);
		} else {
			logger.warn("Position by GPS isn't working correct");
		}
	}

	/**
	 * Helper method to insert track measurement into the database (ensures that
	 * track measurement is only stored every 5 seconds and not faster...)
	 * 
	 * @param insertMeasurement
	 *            The measurement you want to insert
	 */
	private void insertMeasurement(Measurement insertMeasurement) {

		// TODO: This has to be added with the following conditions:
		/*
		 * 1)New measurement if more than 50 meters away 2)New measurement if
		 * last measurement more than 1 minute ago 3)New measurement if MAF
		 * value changed significantly (whatever this means... we will have to
		 * investigate on that. also it is not clear whether we should use this
		 * condition because we are vulnerable to noise from the sensor.
		 * therefore, we should include a minimum time between measurements (1
		 * sec) as well.)
		 */

		if (Math.abs(lastInsertTime - insertMeasurement.getMeasurementTime()) > 5000) {

			lastInsertTime = insertMeasurement.getMeasurementTime();

			track.addMeasurement(insertMeasurement);

			logger.info("Add new measurement to track: " + insertMeasurement.toString());

			Toast.makeText(getApplicationContext(), "" + measurement.getCalculatedMaf(),
						Toast.LENGTH_SHORT).show();

		}

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
		listener = null;
		handler = null;
	}

	/**
	 * updates the location variables when the device moved
	 */
	@Override
	public void onLocationChanged(Location location) {
		locationLatitude = (float) location.getLatitude();
		locationLongitude = (float) location.getLongitude();
		logger.info("Get new position of the GPS_Provider");
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
	 * @return the speedMeasurement
	 */
	public int getSpeedMeasurement() {
		return speedMeasurement;
	}

	/**
	 * @return the track
	 */
	public Track getTrack() {
		return track;
	}

	/**
	 * @param track
	 *            the track to set
	 */
	public void setTrack(Track track) {
		this.track = track;
	}

	/**
	 * 
	 * @return the current co2 value
	 */
	public double getCo2Measurement() {
		return co2Measurement;
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

	/**
	 * @return the imperialUnits
	 */
	public boolean isImperialUnits() {
		return imperialUnits;
	}

	/**
	 * @param imperialUnits the imperialUnits to set
	 */
	public void setImperialUnits(boolean imperialUnits) {
		this.imperialUnits = imperialUnits;
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

}
