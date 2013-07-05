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

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.views.RoundProgress;
import org.envirocar.app.views.TypefaceEC;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
/**
 * Dashboard page that displays the current speed, co2 and car.
 * @author jakob
 * @author gerald
 *
 */
public class DashboardFragment extends SherlockFragment {

	public static final int SENSOR_CHANGED_RESULT = 1337;
	
	// UI Items
	
	TextView speedTextView;
	RoundProgress roundProgressSpeed;
	TextView co2TextView;
	RoundProgress roundProgressCO2;
	DbAdapter dbAdapter;
	ECApplication application;
	private TextView sensor;
	int speed;
	int speedProgress;
	double co2;
	double co2Progress;
	View dashboardView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dashboard, container, false);
	}
	
	/**
	 * Updates the sensor-textview
	 */
	public void updateSensorOnDashboard(){
		sensor.setText(application.getCurrentSensorString());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);
		
		dashboardView = getView();

		// Include application and adapter
		
		application = ((ECApplication) getActivity().getApplication());
		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getDbAdapterLocal();
		
		// Setup UI elements

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		speedTextView = (TextView) getView().findViewById(
				R.id.textViewSpeedDashboard);
		roundProgressCO2 = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);
		roundProgressSpeed = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar2);
		sensor = (TextView) getView().findViewById(R.id.dashboard_current_sensor);
		
		updateSensorOnDashboard();
		
		sensor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        MyGarage garageFragment = new MyGarage();
		        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, garageFragment).addToBackStack(null).commit();
			}
		});
		
		// Handle the UI updates

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				// Deal with the speed values

				speed = application.getSpeedMeasurement();
				if (!application.isImperialUnits()) {
					speedTextView.setText(speed + " km/h");
					if (speed <= 0)
						speedProgress = 0;
					else if (speed > 200)
						speedProgress = 100;
					else
						speedProgress = speed / 2;
					roundProgressSpeed.setProgress(speedProgress);
				} else {
					speedTextView.setText(speed/1.6 + " mph");
					if (speed <= 0)
						speedProgress = 0;
					else if (speed > 150)
						speedProgress = 100;
					else
						speedProgress = (int) (speed / 1.5);
					roundProgressSpeed.setProgress(speedProgress);
				}
				

				// Deal with the co2 values

				co2 = application.getCo2Measurement();
				co2TextView.setText(co2 + " unit"); // TODO work out unit
													// precisely
				if (co2 <= 0)
					co2Progress = 0;
				else if (co2 > 200)
					co2Progress = 100;
				else
					co2Progress = co2 / 2;
				roundProgressCO2.setProgress(co2Progress);
				
				if (co2Progress>60){
					dashboardView.setBackgroundColor(Color.RED);
				} else {
					dashboardView.setBackgroundColor(Color.WHITE);
				}

				// Repeat this in x ms
				handler.postDelayed(this, 1000);
			}
		};
		
		// Repeat the UI update every second (1000ms)
		
		handler.postDelayed(runnable, 1000);

		TypefaceEC.applyCustomFont((ViewGroup) view,
				TypefaceEC.Newscycle(getActivity()));

	}
}
