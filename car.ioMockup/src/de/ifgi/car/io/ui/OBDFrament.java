package de.ifgi.car.io.ui;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.ifgi.obd2.adapter.DbAdapter;
import com.ifgi.obd2.adapter.Measurement;
import com.ifgi.obd2.commands.CommonCommand;
import com.ifgi.obd2.exception.LocationInvalidException;
import com.ifgi.obd2.obd.BackgroundService;
import com.ifgi.obd2.obd.Listener;
import com.ifgi.obd2.obd.ServiceConnector;

import de.ifgi.car.io.R;

public class OBDFrament extends SherlockFragment {
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		LinearLayout l = (LinearLayout) inflater.inflate(R.layout.main, container, false);

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		locationLatitudeTextView = (TextView) l.findViewById(R.id.latitudeText);
		locationLongitudeTextView = (TextView) l.findViewById(R.id.longitudeText);

		// AutoConnect checkbox and service

		final CheckBox connectAutomatically = (CheckBox) l.findViewById(R.id.checkBox1);

		final ScheduledExecutorService scheduleTaskExecutor = Executors
				.newScheduledThreadPool(1);

		connectAutomatically
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (connectAutomatically.isChecked()) {
							// Start Service every minute
							scheduleTaskExecutor.scheduleAtFixedRate(
									new Runnable() {
										public void run() {
											if (requirementsFulfilled) {
												if (!serviceConnector
														.isRunning()) {
													((MainActivity) getActivity()).startConnection();
												} else {
													Log.e("obd2",
															"serviceConnector not running");
												}
											} else {
												Log.e("obd2",
														"requirementsFulfilled was false!");
											}

										}
									}, 0, 1, TimeUnit.MINUTES);

						} else {
							// Stop Service
							scheduleTaskExecutor.shutdown();
						}

					}
				});

		// Toggle Button for WLan Upload

		final ToggleButton wlanToggleButton = (ToggleButton) l.findViewById(R.id.toggleButton1);
		wlanToggleButton.setChecked(true);
		uploadOnlyInWlan = true;

		wlanToggleButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (wlanToggleButton.isChecked()) {
							uploadOnlyInWlan = true;
						} else {
							uploadOnlyInWlan = false;
						}

					}
				});

		// Upload data every 10 minutes and only if there are more than 50
		// measurements stored in the database

		ScheduledExecutorService uploadTaskExecutor = Executors
				.newScheduledThreadPool(1);
		uploadTaskExecutor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(MainActivity.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				Log.e("obd2", "pre uploading");

				if (dbAdapter.getNumberOfStoredMeasurements() > 50) {
					if (uploadOnlyInWlan == true) {
						if (mWifi.isConnected()) {
							// TODO: upload
							Log.e("obd2", "uploading");
						}
					} else {
						// TODO: upload
						Log.e("obd2", "uploading");
					}
				}

			}
		}, 0, 10, TimeUnit.MINUTES);

		/*
		 * Measure with the listener
		 */

		// Try to create an empty measurement

		try {
			measurement = new Measurement(locationLatitude, locationLongitude);
		} catch (LocationInvalidException e) {
			e.printStackTrace();
		}

		// Make a new listener to interpret the measurement values that are
		// returned

		listener = new Listener() {
			
			private View view;
			
			public void setView(View view){

				this.view = view;
			}

			public void receiveUpdate(CommonCommand job) {

				// Get the name and the result of the Command

				String commandName = job.getCommandName();
				String commandResult = job.getResult();

				// Get the fuel type from the preferences

				TextView fuelTypeTextView = (TextView) view.findViewById(R.id.fueltypeText);
				fuelTypeTextView.setText(preferences.getString(PREF_FUEL_TPYE,
						"Gasoline"));

				/*
				 * Check which measurent is returned and save the value in the
				 * previously created measurement
				 */

				// RPM

				if (commandName.equals("Engine RPM")) {
					TextView rpmTextView = (TextView) view.findViewById(R.id.rpm_text);
					rpmTextView.setText(commandResult + " rpm");
					rpmMeasurement = Integer.valueOf(commandResult);

				}

				// Speed

				else if (commandName.equals("Vehicle Speed")) {
					TextView speedTextView = (TextView) view.findViewById(R.id.spd_text);
					speedTextView.setText(commandResult + " km/h");

					try {
						speedMeasurement = Integer.valueOf(commandResult);
					} catch (NumberFormatException e) {
						Log.e("obd2", "speed parse exception");
						e.printStackTrace();
					}
				}

				// Short Term Trim Bank 1

				else if (commandName.equals("Short Term Fuel Trim Bank 1")) {
					TextView shortTermTrimTextView = (TextView) view.findViewById(R.id.shortTrimText);
					String shortTermTrimBank1 = commandResult;
					shortTermTrimTextView.setText("Short Term Trim: "
							+ shortTermTrimBank1 + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(shortTermTrimBank1);
						shortTermTrimBank1Measurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception short term");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception short term");
						e.printStackTrace();
					}
				}

				// Long Term Trim Bank 1

				else if (commandName.equals("Long Term Fuel Trim Bank 1")) {
					TextView longTermTrimTextView = (TextView) view.findViewById(R.id.longTrimText);
					String longTermTrimBank1 = commandResult;
					longTermTrimTextView.setText("Long Term Trim: "
							+ longTermTrimBank1 + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(longTermTrimBank1);
						longTermTrimBank1Measurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception long term");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception long term");
						e.printStackTrace();
					}
				}

				// Intake Temperature

				else if (commandName.equals("Air Intake Temperature")) {
					TextView intakeTempTextView = (TextView) view.findViewById(R.id.intakeTempText);
					String intakeTemperature = commandResult;
					intakeTempTextView.setText("Intake Temp: "
							+ intakeTemperature + " C");
					try {
						intakeTemperatureMeasurement = Integer
								.valueOf(intakeTemperature);
					} catch (NumberFormatException e) {
						Log.e("obd2", "intake temp parse exception");
						e.printStackTrace();
					}

				}

				// Throttle Position

				else if (commandName.equals("Throttle Position")) {
					TextView throttlePositionTextView = (TextView) view.findViewById(R.id.throttle);
					String throttlePosition = commandResult;
					throttlePositionTextView.setText("T. Pos: "
							+ throttlePosition + " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(throttlePosition);
						throttlePositionMeasurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception throttle");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception throttle");
						e.printStackTrace();
					}

				}

				// Engine Load

				else if (commandName.equals("Engine Load")) {
					TextView engineLoadTextView = (TextView) view.findViewById(R.id.engineLoadText);
					String engineLoad = commandResult;
					Log.e("obd2", "Engine Load: " + engineLoad);
					engineLoadTextView.setText("Engine load: " + engineLoad
							+ " %");

					try {
						NumberFormat format = NumberFormat
								.getInstance(Locale.GERMAN);
						Number number;
						number = format.parse(engineLoad);
						engineLoadMeasurement = number.doubleValue();
					} catch (ParseException e) {
						Log.e("obd2", "parse exception load");
						e.printStackTrace();
					} catch (java.text.ParseException e) {
						Log.e("obd2", "parse exception load");
						e.printStackTrace();
					}
				}

				// MAF

				else if (commandName.equals("Mass Air Flow")) {
					TextView mafTextView = (TextView) view.findViewById(R.id.mafText);
					String maf = commandResult;
					mafTextView.setText("MAF: " + maf + " g/s");

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

				// Intake Pressure

				else if (commandName.equals("Intake Manifold Pressure")) {
					TextView intakePressureTextView = (TextView) view.findViewById(R.id.intakeText);
					String intakePressure = commandResult;
					intakePressureTextView.setText("Intake: " + intakePressure
							+ "kPa");

					try {
						intakePressureMeasurement = Integer
								.valueOf(intakePressure);
					} catch (NumberFormatException e) {
						Log.e("obd", "intake pressure parse exception");
						e.printStackTrace();
					}
				}

				// Update and insert the measurement

				((MainActivity) getActivity()).updateMeasurement();
			}
			
		};
		listener.setView(l);
		// Get the default bluetooth adapter

		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		// Check whether the bluetooth adapter is available or supported

		if (bluetoothAdapter == null) {

			requirementsFulfilled = false;
			((MainActivity) getActivity()).showDialog(NO_BLUETOOTH);

		} else {

			if (!bluetoothAdapter.isEnabled()) {
				requirementsFulfilled = false;
				((MainActivity) getActivity()).showDialog(BLUETOOTH_DISABLED);
			}
		}

		// If everything is available, start the service connector and listener

		if (requirementsFulfilled) {
			backgroundService = new Intent(getActivity(), BackgroundService.class);
			serviceConnector = new ServiceConnector();
			serviceConnector.setServiceListener(listener);

			((MainActivity) getActivity()).bindService(backgroundService, serviceConnector,
					Context.BIND_AUTO_CREATE);
		}		

		
		return l;

	}

	public ServiceConnector getServiceConnector(){
		return serviceConnector;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
	}
}
