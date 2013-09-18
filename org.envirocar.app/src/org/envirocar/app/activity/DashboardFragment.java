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

import java.text.DecimalFormat;

import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver;
import org.envirocar.app.application.service.BackgroundServiceImpl;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
import org.envirocar.app.event.CO2Event;
import org.envirocar.app.event.CO2EventListener;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.event.LocationEventListener;
import org.envirocar.app.event.SpeedEvent;
import org.envirocar.app.event.SpeedEventListener;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.views.RoundProgress;
import org.envirocar.app.views.TypefaceEC;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
/**
 * Dashboard page that displays the current speed, co2 and car.
 * @author jakob
 * @author gerald
 *
 */
public class DashboardFragment extends SherlockFragment {

	private static final Logger logger = Logger.getLogger(DashboardFragment.class);
	public static final int SENSOR_CHANGED_RESULT = 1337;
	private static final String SERVICE_STATE = "serviceState";
	private static final String LOCATION = "location";
	private static final String SPEED = "speed";
	private static final String CO2 = "co2";
	
	// UI Items
	
	TextView speedTextView;
	RoundProgress roundProgressSpeed;
	TextView co2TextView;
	TextView positionTextView;
	RoundProgress roundProgressCO2;
	private TextView sensor;
	View dashboardView;

	private LocationEventListener locationListener;
	private SpeedEventListener speedListener;
	private CO2EventListener co2Listener;

	private SharedPreferences preferences;

	private long lastUIUpdate;
	private int speed;
	private Location location;
	private double co2;

	private BroadcastReceiver receiver;
	protected ServiceState serviceState = ServiceState.SERVICE_STOPPED;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		logger.info("onCreateView. hash="+System.identityHashCode(this));
		return inflater.inflate(R.layout.dashboard, container, false);
	}
	
	/**
	 * Updates the sensor-textview
	 */
	public void updateSensorOnDashboard(){
		sensor.setText(getCurrentSensorString());
	}
	
	/**
	 * Returns the sensor properties as a string
	 * @return
	 */
	private String getCurrentSensorString() {
		if (CarManager.instance().isCarSet()) {
			Car car = CarManager.instance().getCar();
			return car.getManufacturer()+" "+car.getModel()+" ("+car.getFuelType().toString()+" "+car.getConstructionYear()+")";
		} else {
			return getResources().getString(R.string.no_sensor_selected);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		readSavedState(savedInstanceState);
		
		logger.info("onViewCreated. hash="+System.identityHashCode(this));
		
		initializeEventListeners();
		
		dashboardView = getView();

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		
		// Setup UI elements

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		speedTextView = (TextView) getView().findViewById(
				R.id.textViewSpeedDashboard);
		roundProgressCO2 = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);
		roundProgressSpeed = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar2);
		sensor = (TextView) getView().findViewById(R.id.dashboard_current_sensor);
		
		positionTextView = (TextView) getView().findViewById(R.id.positionTextView);
		
		updateSensorOnDashboard();
		
		updateStatusElements();
		
		sensor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        MyGarage garageFragment = new MyGarage();
		        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, garageFragment).addToBackStack(null).commit();
			}
		});
		
		TypefaceEC.applyCustomFont((ViewGroup) view,
				TypefaceEC.Newscycle(getActivity()));
		
		receiver = new AbstractBackgroundServiceStateReceiver() {

			@Override
			public void onStateChanged(ServiceState state) {
				serviceState = state;
				updateStatusElements();
			}
		};
		getActivity().registerReceiver(receiver, new IntentFilter(AbstractBackgroundServiceStateReceiver.SERVICE_STATE));

	}
	

	@Override
	public void onDestroy() {
		logger.info("onDestroy. hash="+System.identityHashCode(this));
		super.onDestroy();
		
		getActivity().unregisterReceiver(receiver);
		
		EventBus.getInstance().unregisterListener(this.locationListener);
		EventBus.getInstance().unregisterListener(this.speedListener);
		EventBus.getInstance().unregisterListener(this.co2Listener);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable(SERVICE_STATE, serviceState);
		outState.putParcelable(LOCATION, location);
		outState.putInt(SPEED, speed);
		outState.putDouble(CO2, co2);
	}

	private void readSavedState(Bundle savedInstanceState) {
		if (savedInstanceState == null) return;
		
		this.serviceState = (ServiceState) savedInstanceState.getSerializable(SERVICE_STATE);
		this.location = savedInstanceState.getParcelable(LOCATION);
		this.speed = savedInstanceState.getInt(SPEED);
		this.co2 = savedInstanceState.getDouble(CO2);
		
		BackgroundServiceImpl.requestServiceStateBroadcast(getActivity());
	}
	
	@Override
	public void onStop() {
		logger.info("onStop. hash="+System.identityHashCode(this));
		super.onStop();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		logger.info("onCreate. hash="+System.identityHashCode(this));
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onPause() {
		logger.info("onPause. hash="+System.identityHashCode(this));
		super.onPause();
	}
	
	@Override
	public void onDestroyView() {
		logger.info("onDestroyView. hash="+System.identityHashCode(this));
		super.onDestroyView();
	}
	
	@Override
	public void onStart() {
		logger.info("onStart. hash="+System.identityHashCode(this));
		super.onStart();
	}
	
	@Override
	public void onResume() {
		logger.info("onResume. hash="+System.identityHashCode(this));
		super.onResume();
	}

	private void initializeEventListeners() {
		this.locationListener = new LocationEventListener() {
			@Override
			public void receiveEvent(LocationEvent event) {
				updateLocation(event.getPayload());
			}
		};
		this.speedListener = new SpeedEventListener() {
			@Override
			public void receiveEvent(SpeedEvent event) {
				updateSpeed(event.getPayload());
			}
		};
		this.co2Listener = new CO2EventListener() {
			@Override
			public void receiveEvent(CO2Event event) {
				updateCO2(event.getPayload());
			}
		};
		EventBus.getInstance().registerListener(locationListener);
		EventBus.getInstance().registerListener(speedListener);
		EventBus.getInstance().registerListener(co2Listener);
		
		lastUIUpdate = System.currentTimeMillis();
	}

	protected void updateCO2(final Double co2) {
		this.co2 = co2;
		checkUIUpdate();
	}

	protected void updateSpeed(final Integer speed) {
		this.speed = speed;
		checkUIUpdate();
	}

	protected void updateLocation(final Location location) {
		this.location = location;
		checkUIUpdate();
	}
	
	protected void updateStatusElements() {
		if (getView() == null) return;
		
		ImageView connectionStateImage = (ImageView) getView().findViewById(R.id.connectionStateImage);
		
		if (connectionStateImage == null) return;
		
		if (serviceState == ServiceState.SERVICE_STARTED) {
			connectionStateImage.setImageResource(R.drawable.connection_state_true);
		}
		else if (serviceState == ServiceState.SERVICE_STARTING) {
			connectionStateImage.setImageResource(R.drawable.connection_state_stale);
		}
		else {
			connectionStateImage.setImageResource(R.drawable.connection_state_false);
			co2 = 0.0;
			speed = 0;
			updateCo2Value();
			updateSpeedValue();
		}
		
	}
	
	private synchronized void checkUIUpdate() {
		if (serviceState == ServiceState.SERVICE_STOPPED) return;
		
		if (getActivity() == null || System.currentTimeMillis() - lastUIUpdate < 250) return;
		
		lastUIUpdate = System.currentTimeMillis();
		
		if (location != null || speed != 0 || co2 != 0.0) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateLocationValue();
					
					updateSpeedValue();
					
					updateCo2Value();			
				}
			});
		}
	}

	protected void updateCo2Value() {
		double co2Progress;
		
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		
		co2TextView.setText(twoDForm.format(co2) + " kg/h"); 
		if (co2 <= 0)
			co2Progress = 0;
		else if (co2 > 100)
			co2Progress = 100;
		else
			co2Progress = co2;
		roundProgressCO2.setProgress(co2Progress);
		
		if (co2Progress>30){
			dashboardView.setBackgroundColor(Color.RED);
		} else {
			dashboardView.setBackgroundColor(Color.WHITE);
		}
	}

	protected void updateSpeedValue() {
		int speedProgress;
		if (!preferences.getBoolean(SettingsActivity.IMPERIAL_UNIT,
				false)) {
			speedTextView.setText(speed + " km/h");
			if (speed <= 0)
				speedProgress = 0;
			else if (speed > 200)
				speedProgress = 100;
			else
				speedProgress = speed / 2;
			roundProgressSpeed.setProgress(speedProgress);
		} else {
			speedTextView.setText(speed / 1.6 + " mph");
			if (speed <= 0)
				speedProgress = 0;
			else if (speed > 150)
				speedProgress = 100;
			else
				speedProgress = (int) (speed / 1.5);
			roundProgressSpeed.setProgress(speedProgress);
		}
	}

	protected void updateLocationValue() {
		if (location != null && location.getLongitude() != 0
				&& location.getLatitude() != 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("Provider: " + location.getProvider() + "\n");
			sb.append("Lat: " + location.getLatitude() + "\n");
			sb.append("Long: " + location.getLongitude() + "\n");
			sb.append("Acc: " + location.getAccuracy() + "\n");
			sb.append("Speed: " + location.getSpeed() + "\n");
			positionTextView.setText(sb.toString());
			positionTextView.setTextColor(Color.BLACK);
			positionTextView.setBackgroundColor(Color.WHITE);
		} else {
			positionTextView.setText(R.string.positioning_Info);
			positionTextView.setTextColor(Color.WHITE);
			positionTextView.setBackgroundColor(Color.RED);
		}
	}

}
