package car.io.application;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import car.io.adapter.DbAdapter;
import car.io.adapter.DbAdapterLocal;
import car.io.adapter.DbAdapterRemote;
import car.io.adapter.Measurement;
import car.io.adapter.Track;
import car.io.commands.CommonCommand;
import car.io.commands.MAF;
import car.io.commands.Speed;
import car.io.exception.LocationInvalidException;
import car.io.obd.BackgroundService;
import car.io.obd.Listener;
import car.io.obd.ServiceConnector;

public class ECApplication extends Application implements LocationListener {
	
	
	public static final String GET_TRACKS_URI = "http://giv-car.uni-muenster.de:8080/stable/rest/tracks";

	private static ECApplication singleton;
	private DbAdapter dbAdapterLocal;
	private DbAdapter dbAdapterRemote;
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
		startBackgroundService();
		//startServiceConnector();

		track = new Track("123456", "Gasoline", dbAdapterLocal); //TODO create track dynamically and from preferences

		try {
			measurement = new Measurement(locationLatitude, locationLongitude);
		} catch (LocationInvalidException e) {
			e.printStackTrace();
		}
		
		downloadTracks();

		singleton = this;
	}

	private void initDbAdapter() {
		if (dbAdapterLocal == null) {
			dbAdapterLocal = new DbAdapterLocal(this.getApplicationContext());
			dbAdapterLocal.open();
		} else {
			if(!dbAdapterLocal.isOpen()) dbAdapterLocal.open();
		}
		if (dbAdapterRemote == null) {
			dbAdapterRemote = new DbAdapterRemote(this.getApplicationContext());
			dbAdapterRemote.open();
		} else {
			if(!dbAdapterRemote.isOpen()) dbAdapterRemote.open();
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

	public void downloadTracks(){
		
		dbAdapterRemote.deleteAllTracks();
		AsyncTask<Void, Void, Void> downloadTracksTask = new AsyncTask<Void, Void, Void>(){
			
			
			


			JSONParser parser = new JSONParser();
			JSONArray track = null;
			JSONArray measurementsJSONArray = null;
			JSONObject eachTrackJSON = new JSONObject();
			Track trackToInsert = null;
			ArrayList<Measurement> measurements = new ArrayList<Measurement>();
			
			SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			
			long start;
			
			
			protected void onPreExecute() {
				start = System.currentTimeMillis();
			};
			
			@Override
			protected Void doInBackground(Void... params) {
				String response = HttpRequest.get(GET_TRACKS_URI).body();
				try {
					track = (JSONArray) ((JSONObject) parser.parse(response)).get("tracks");
					for(int i = 0; i<track.size(); i++){
						//TODO skip tracks already in the database
						
						trackToInsert = new Track((String) ((JSONObject) track.get(i)).get("id"));
						trackToInsert.setName((String) ((JSONObject) track.get(i)).get("name"));
						//download the uri supplied by the 'tracks'-request
						String eachTrackResponse = HttpRequest.get((CharSequence) ((JSONObject) track.get(i)).get("href")).body();
						//decode..
						eachTrackJSON = (JSONObject) parser.parse(eachTrackResponse);
						
						//fill out the blanks..
						trackToInsert.setDescription((String) ((JSONObject) eachTrackJSON.get("properties")).get("description"));
						//TODO more properties when ready
						
						//Fill the measurements
						//TODO replace with actual data
						measurementsJSONArray = ((JSONArray) eachTrackJSON.get("features")); //TODO make this whole process asynchronous.. takes 108 seconds on htc desire
						for(Object m : measurementsJSONArray){
							try {
								Measurement measurement = new Measurement(((Number) ((JSONArray) ((JSONObject) ((JSONObject) m).get("geometry")).get("coordinates")).get(1)).floatValue(),  ((Number) ((JSONArray) ((JSONObject) ((JSONObject) m).get("geometry")).get("coordinates")).get(0)).floatValue());
								measurement.setMaf(((Number) ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) m).get("properties")).get("phenomenons")).get("testphenomenon1")).get("value")).floatValue());
								measurement.setSpeed(((Number) ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) m).get("properties")).get("phenomenons")).get("testphenomenon2")).get("value")).intValue());
								measurement.setMeasurementTime(sdf.parse((String) ((JSONObject) ((JSONObject) m).get("properties")).get("time")).getTime());//TODO look into date
								measurement.setTrack(trackToInsert);
								
								//add to measurements
								measurements.add(measurement);
							} catch (NumberFormatException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (LocationInvalidException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (java.text.ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						//finally add the measurements to the track and insert to the database
						trackToInsert.setMeasurementsAsArrayList(measurements);
						((DbAdapterRemote) dbAdapterRemote).insertTrackWithMeasurements(trackToInsert);
					}

				} catch (org.json.simple.parser.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				Log.i("duration",(System.currentTimeMillis()-start)+"");
				Log.i("remoteTrack",dbAdapterRemote.getNumberOfStoredTracks()+"");
			}
		};
		downloadTracksTask.execute((Void)null);
	}
	
	public DbAdapter getDbAdapterLocal() {
		initDbAdapter();
		return dbAdapterLocal;
	}
	
	public DbAdapter getDbAdapterRemote() {
		initDbAdapter();
		return dbAdapterRemote;
	}
	
	public void stopLocating(){
		locationManager.removeUpdates(this);
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

		// Create track new measurement if necessary

		if (measurement == null) {
			try {
				measurement = new Measurement(locationLatitude,
						locationLongitude);
			} catch (LocationInvalidException e) {
				e.printStackTrace();
			}
		}

		// Insert the values if the measurement (with the coordinates) is young
		// enough (5000ms) or create track new one if it is too old

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
	 * Helper method to insert track measurement into the database (ensures that track
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
	
	
	public void openDb(){
		initDbAdapter();
	}

	public void closeDb() {
		if(dbAdapterLocal != null){
			dbAdapterLocal.close();
			dbAdapterLocal = null;
		}
		if(dbAdapterRemote != null){
			dbAdapterRemote.close();
			dbAdapterRemote = null;
		}
		
	}

}
