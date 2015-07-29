///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//
//package org.envirocar.app.activity;
//
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import android.graphics.Color;
//import android.graphics.drawable.Drawable;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.squareup.otto.Bus;
//
//import org.envirocar.app.injection.BaseInjectorFragment;
//import org.envirocar.app.R;
//import org.envirocar.app.application.CarManager;
//import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver;
//import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
//import org.envirocar.app.application.service.BackgroundServiceImpl;
//import org.envirocar.app.application.service.BackgroundServiceInteractor;
//import org.envirocar.app.event.CO2Event;
//import org.envirocar.app.event.CO2EventListener;
//import org.envirocar.app.event.EventBus;
//import org.envirocar.app.events.GpsSatelliteFix;
//import org.envirocar.app.events.GpsSatelliteFixEvent;
//import org.envirocar.app.event.GpsSatelliteFixEventListener;
//import org.envirocar.app.event.LocationEvent;
//import org.envirocar.app.event.LocationEventListener;
//import org.envirocar.app.event.SpeedEvent;
//import org.envirocar.app.event.SpeedEventListener;
//import org.envirocar.app.logging.Logger;
//import org.envirocar.app.model.Car;
//import org.envirocar.app.model.Car.FuelType;
//import org.envirocar.app.views.LayeredImageRotateView;
//import org.envirocar.app.views.TypefaceEC;
//
//import java.text.DecimalFormat;
//
//import javax.inject.Inject;
//
//import de.keyboardsurfer.android.widget.crouton.Crouton;
///**
// * Dashboard page that displays the current speed, co2 and car.
// * @author jakob
// * @author gerald
// *
// */
//public class DashboardFragment extends BaseInjectorFragment {
//
//	private static final Logger logger = Logger.getLogger(DashboardFragment.class);
//	public static final int SENSOR_CHANGED_RESULT = 1337;
////	private static final String SERVICE_STATE = "serviceState";
//	private static final String LOCATION = "location";
//	private static final String SPEED = "speed";
//	private static final String CO2 = "co2";
//	private static DecimalFormat twoDForm = new DecimalFormat("#.##");
//
//	// UI Items
//
//	TextView speedTextView;
//	TextView co2TextView;
//	View dashboardView;
//
//	private LocationEventListener locationListener;
//	private SpeedEventListener speedListener;
//	private CO2EventListener co2Listener;
//
//	private SharedPreferences preferences;
//
//	private long lastUIUpdate;
//	private int speed;
//	private Location location;
//	private double co2;
//
//	private BroadcastReceiver receiver;
//	protected ServiceState serviceState = ServiceState.SERVICE_STOPPED;
//	private OnSharedPreferenceChangeListener preferenceListener;
//	private LayeredImageRotateView speedRotatableView;
//	private LayeredImageRotateView co2RotableView;
//	protected BackgroundServiceInteractor backgroundService;
//	private GpsSatelliteFixEventListener gpsFixListener;
//	private GpsSatelliteFix fix = new GpsSatelliteFix(0, false);
//	private ImageView gpsFixView;
//	private ImageView carOkView;
//	private Drawable carOkDrawable;
//	private Drawable carNotOkDrawable;
//	private Drawable gpsFix;
//	private Drawable gpsNoFix;
//	private Drawable btNotSelected;
//	private Drawable btStopped;
//	private Drawable btPending;
//	private Drawable btActive;
//	private ImageView connectionStateImage;
//	private boolean useImperialUnits;
//
//	@Inject
//	protected Bus mBus;
//	@Inject
//	protected CarManager mCarManager;
//
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container,
//			Bundle savedInstanceState) {
//		logger.info("onCreateView. hash="+System.identityHashCode(this));
//
//
//		return inflater.inflate(R.layout.dashboard, container, false);
//	}
//
//	/**
//	 * Updates the sensor-textview
//	 */
//	private void updateGpsStatus() {
//		if (fix.isFix()) {
//			gpsFixView.setImageDrawable(gpsFix);
//		}
//		else {
//			gpsFixView.setImageDrawable(gpsNoFix);
//		}
//	}
//
//	private void updateCarStatus() {
//		if (mCarManager.getCar() != null) {
//			carOkView.setImageDrawable(carOkDrawable);
//		}
//		else {
//			carOkView.setImageDrawable(carNotOkDrawable);
//		}
//	}
//
//
//	@Override
//	public void onViewCreated(View view, Bundle savedInstanceState) {
//		super.onViewCreated(view, savedInstanceState);
//
//		loadCommonDrawables();
//
//		readSavedState(savedInstanceState);
//
//		logger.info("onViewCreated. hash="+System.identityHashCode(this));
//
//		dashboardView = getView();
//
//		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
//
//		// Setup UI elements
//
//		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
//		speedTextView = (TextView) getView().findViewById(
//				R.id.textViewSpeedDashboard);
//		co2RotableView = (LayeredImageRotateView) getView().findViewById(
//				R.id.co2meterView);
//		speedRotatableView = (LayeredImageRotateView) getView().findViewById(R.id.speedometerView);
//
//		/*
//		 * status images
//		 */
//		setupStatusImages();
//
//		updateStatusElements();
//
//		TypefaceEC.applyCustomFont((ViewGroup) view,
//				TypefaceEC.Newscycle(getActivity()));
//
//		receiver = new AbstractBackgroundServiceStateReceiver() {
//
//			@Override
//			public void onStateChanged(ServiceState state) {
//				serviceState = state;
//				updateStatusElements();
//			}
//		};
//		getActivity().registerReceiver(receiver, new IntentFilter(AbstractBackgroundServiceStateReceiver.SERVICE_STATE));
//
//		preferenceListener = new OnSharedPreferenceChangeListener() {
//			@Override
//			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
//					String key) {
//				if (key.equals(SettingsActivity.PREFERENCE_TAG_CAR) || key.equals(SettingsActivity.CAR_HASH_CODE)) {
//					updateCarStatus();
//				}
//				else if (key.equals(SettingsActivity.BLUETOOTH_KEY)) {
//					updateStatusElements();
//				}
//			}
//		};
//
//
//		preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
//
//		bindToBackgroundService();
//	}
//
//
//
//	private void setupStatusImages() {
//		gpsFixView = (ImageView) getView().findViewById(R.id.gpsFixView);
//
//		carOkView = (ImageView) getView().findViewById(R.id.carOkView);
//		carOkView.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Car car = mCarManager.getCar();
//				if (car != null) {
//					Toast.makeText(getActivity(), car.toString(), Toast.LENGTH_SHORT).show();
//				}
//				else {
//					Toast.makeText(getActivity(), R.string.no_sensor_selected, Toast.LENGTH_SHORT).show();
//				}
//			}
//		});
//
//		connectionStateImage = (ImageView) getView().findViewById(R.id.connectionStateImage);
//		connectionStateImage.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				String remoteDevice = preferences.getString(
//						org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,
//						null);
//
//				if (remoteDevice == null) {
//					Toast.makeText(getActivity(), R.string.no_device_selected, Toast.LENGTH_SHORT).show();
//				}
//			}
//		});
//	}
//
//	private void loadCommonDrawables() {
//		carOkDrawable = getResources().getDrawable(R.drawable.car_ok);
//		carNotOkDrawable = getResources().getDrawable(R.drawable.car_no);
//		gpsFix = getResources().getDrawable(R.drawable.gps_fix);
//		gpsNoFix = getResources().getDrawable(R.drawable.gps_nofix);
//		btNotSelected = getResources().getDrawable(R.drawable.bt_device_not_selected);
//		btStopped = getResources().getDrawable(R.drawable.bt_device_stopped);
//		btPending = getResources().getDrawable(R.drawable.bt_device_pending);
//		btActive = getResources().getDrawable(R.drawable.bt_device_active);
//	}
//
//	@Override
//	public void onDestroy() {
//		logger.info("onDestroy. hash="+System.identityHashCode(this));
//		super.onDestroy();
//
//		try {
//			getActivity().unregisterReceiver(receiver);
//		}
//		catch (IllegalArgumentException e) {
//			logger.warn(e.getMessage(), e);
//			logger.warn("Reconsider the Receiver registration lifecycle!");
//		}
//		if (preferences != null) {
//			preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
//		}
//
//	}
//
//	@Override
//	public void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//
////		outState.putSerializable(SERVICE_STATE, serviceState);
//		outState.putParcelable(LOCATION, location);
//		outState.putInt(SPEED, speed);
//		outState.putDouble(CO2, co2);
//	}
//
//	private void readSavedState(Bundle savedInstanceState) {
//		if (savedInstanceState == null) return;
//
////		this.serviceState = (ServiceState) savedInstanceState.getSerializable(SERVICE_STATE);
//		this.location = savedInstanceState.getParcelable(LOCATION);
//		this.speed = savedInstanceState.getInt(SPEED);
//		this.co2 = savedInstanceState.getDouble(CO2);
//	}
//
//	@Override
//	public void onStop() {
//		logger.info("onStop. hash="+System.identityHashCode(this));
//		super.onStop();
//	}
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		logger.info("onCreate. hash="+System.identityHashCode(this));
//		super.onCreate(savedInstanceState);
//
//	}
//
//	private void bindToBackgroundService() {
//		if (!getActivity().bindService(new Intent(this.getActivity(), BackgroundServiceImpl.class),
//				new ServiceConnection() {
//
//					@Override
//					public void onServiceDisconnected(ComponentName name) {
//						logger.info(String.format("BackgroundService %S disconnected!", name.flattenToString()));
//					}
//
//					@Override
//					public void onServiceConnected(ComponentName name, IBinder service) {
//						backgroundService = (BackgroundServiceInteractor) service;
//						serviceState = backgroundService.getServiceState();
//						updateStatusElements();
//					}
//				}, 0)) {
//			logger.warn("Could not connect to BackgroundService.");
//		}
//	}
//
//	@Override
//	public void onPause() {
//		logger.info("onPause. hash="+System.identityHashCode(this));
//		super.onPause();
//
//		EventBus.getInstance().unregisterListener(this.locationListener);
//		EventBus.getInstance().unregisterListener(this.speedListener);
//		EventBus.getInstance().unregisterListener(this.co2Listener);
//		EventBus.getInstance().unregisterListener(this.gpsFixListener);
//	}
//
//	@Override
//	public void onDestroyView() {
//		logger.info("onDestroyView. hash="+System.identityHashCode(this));
//		super.onDestroyView();
//	}
//
//	@Override
//	public void onStart() {
//		logger.info("onStart. hash="+System.identityHashCode(this));
//
//		super.onStart();
//	}
//
//	@Override
//	public void onResume() {
//		logger.info("onResume. hash="+System.identityHashCode(this));
//		super.onResume();
//
//		initializeEventListeners();
//
//		updateGpsStatus();
//
//		updateCarStatus();
//
//		Car car = mCarManager.getCar();
//		if (car != null && car.getFuelType() == FuelType.DIESEL) {
//			Crouton.makeText(getActivity(), R.string.diesel_not_yet_supported,
//					de.keyboardsurfer.android.widget.crouton.Style.ALERT).show();
//		}
//
//		bindToBackgroundService();
//
//		useImperialUnits = preferences.getBoolean(SettingsActivity.IMPERIAL_UNIT,
//				false);
//	}
//
//	private void initializeEventListeners() {
//		this.locationListener = new LocationEventListener() {
//			@Override
//			public void receiveEvent(LocationEvent event) {
//				updateLocation(event.getPayload());
//			}
//		};
//		this.speedListener = new SpeedEventListener() {
//			@Override
//			public void receiveEvent(SpeedEvent event) {
//				updateSpeed(event.getPayload());
//			}
//		};
//		this.co2Listener = new CO2EventListener() {
//			@Override
//			public void receiveEvent(CO2Event event) {
//				updateCO2(event.getPayload());
//			}
//		};
//		this.gpsFixListener = new GpsSatelliteFixEventListener() {
//			@Override
//			public void receiveEvent(GpsSatelliteFixEvent event) {
//				updateGpsState(event.getPayload());
//			}
//		};
//		EventBus.getInstance().registerListener(locationListener);
//		EventBus.getInstance().registerListener(speedListener);
//		EventBus.getInstance().registerListener(co2Listener);
//		EventBus.getInstance().registerListener(gpsFixListener);
//
//		lastUIUpdate = System.currentTimeMillis();
//	}
//
//	protected void updateCO2(final Double co2) {
//		this.co2 = co2;
//		checkUIUpdate();
//	}
//
//	protected void updateGpsState(final GpsSatelliteFix fix) {
//		if (this.fix == null || this.fix.isFix() != fix.isFix()) {
//			this.fix = fix;
//
//			getActivity().runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					updateGpsStatus();
//				}
//			});
//		}
//		else {
//			this.fix = fix;
//		}
//	}
//
//	protected void updateSpeed(final Integer speed) {
//		this.speed = speed;
//		checkUIUpdate();
//	}
//
//	protected void updateLocation(final Location location) {
//		this.location = location;
//		checkUIUpdate();
//	}
//
//	protected void updateStatusElements() {
//		if (getView() == null || !isAdded()) return;
//
//		if (connectionStateImage == null) return;
//
//		String remoteDevice = preferences.getString(
//				org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,
//				null);
//
//		if (remoteDevice == null) {
//			connectionStateImage.setImageDrawable(btNotSelected);
//		}
//		else if (serviceState == ServiceState.SERVICE_STARTED) {
//			connectionStateImage.setImageDrawable(btActive);
//		}
//		else if (serviceState == ServiceState.SERVICE_STARTING) {
//			connectionStateImage.setImageDrawable(btPending);
//		}
//		else {
//			connectionStateImage.setImageDrawable(btStopped);
//			co2 = 0.0;
//			speed = 0;
//			updateCo2Value();
//			updateSpeedValue();
//		}
//
//	}
//
//	private synchronized void checkUIUpdate() {
//		if (serviceState == ServiceState.SERVICE_STOPPED) return;
//
//		if (getActivity() == null || System.currentTimeMillis() - lastUIUpdate < 250) return;
//
//		lastUIUpdate = System.currentTimeMillis();
//
//		if (location != null || speed != 0 || co2 != 0.0) {
//			getActivity().runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					updateSpeedValue();
//
//					updateCo2Value();
//				}
//			});
//		}
//	}
//
//	protected void updateCo2Value() {
//		co2TextView.setText(twoDForm.format(co2) + " kg/h");
//		co2RotableView.submitScaleValue((float) co2);
//
//		if (co2 > 30){
//			dashboardView.setBackgroundColor(Color.RED);
//		} else {
//			dashboardView.setBackgroundColor(Color.WHITE);
//		}
//	}
//
//	protected void updateSpeedValue() {
//		if (!useImperialUnits) {
//			speedTextView.setText(speed + " km/h");
//			speedRotatableView.submitScaleValue(speed);
//		} else {
//			speedTextView.setText(speed / 1.6f + " mph");
//			speedRotatableView.submitScaleValue(speed/1.6f);
//		}
//	}
//
//
//}
