package car.io.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.application.ECApplication;
import car.io.views.RoundProgress;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;

public class DashboardFragment extends SherlockFragment {

	
	public static final int SENSOR_CHANGED_RESULT = 1337;
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
	

	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dashboard, container, false);
	}
	
	public void updateSensorOnDashboard(){
		sensor.setText(getCurrentSensorString());
	}
	
	private String getCurrentSensorString(){
		if(PreferenceManager.getDefaultSharedPreferences(application).contains(ECApplication.PREF_KEY_SENSOR_ID) && 
				PreferenceManager.getDefaultSharedPreferences(application).contains(ECApplication.PREF_KEY_FUEL_TYPE) &&
				PreferenceManager.getDefaultSharedPreferences(application).contains(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR) &&
				PreferenceManager.getDefaultSharedPreferences(application).contains(ECApplication.PREF_KEY_CAR_MODEL) &&
				PreferenceManager.getDefaultSharedPreferences(application).contains(ECApplication.PREF_KEY_CAR_MANUFACTURER)){
			String prefSensorid = PreferenceManager.getDefaultSharedPreferences(application).getString(ECApplication.PREF_KEY_SENSOR_ID, "nosensor");
			String prefFuelType = PreferenceManager.getDefaultSharedPreferences(application).getString(ECApplication.PREF_KEY_FUEL_TYPE, "nosensor");
			String prefYear = PreferenceManager.getDefaultSharedPreferences(application).getString(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR, "nosensor");
			String prefModel = PreferenceManager.getDefaultSharedPreferences(application).getString(ECApplication.PREF_KEY_CAR_MODEL, "nosensor");
			String prefManu = PreferenceManager.getDefaultSharedPreferences(application).getString(ECApplication.PREF_KEY_CAR_MANUFACTURER, "nosensor");
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

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);

		application = ((ECApplication) getActivity().getApplication());

		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getDbAdapterLocal();

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
				if(application.isLoggedIn()){
					getActivity().startActivityForResult(new Intent(getActivity(), MyGarage.class), MainActivity.REQUEST_MY_GARAGE);
				}else{
					Intent i = new Intent(getActivity(), LoginActivity.class);
					i.putExtra("redirect", MainActivity.REQUEST_MY_GARAGE);
					getActivity().startActivityForResult(i,MainActivity.REQUEST_REDIRECT_TO_GARAGE);
				}
			}
		});
		

		// Handle the UI updates

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				// Deal with the speed values

				speed = application.getSpeedMeasurement();
				speedTextView.setText(speed + " km/h");
				if (speed <= 0)
					speedProgress = 0;
				else if (speed > 200)
					speedProgress = 100;
				else
					speedProgress = speed / 2;
				roundProgressSpeed.setProgress(speedProgress);

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

				// Repeat this in x ms
				handler.postDelayed(this, 1000);
			}
		};
		handler.postDelayed(runnable, 1000);

		TYPEFACE.applyCustomFont((ViewGroup) view,
				TYPEFACE.Newscycle(getActivity()));

	}
}
