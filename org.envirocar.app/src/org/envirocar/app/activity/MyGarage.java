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

import java.util.ArrayList;

import org.apache.http.Header;
import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.network.RestClient;
import org.envirocar.app.views.TypefaceEC;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Garage class that cares about the sensor type "car" and its properties.
 * 
 * @author gerald
 * 
 */
public class MyGarage extends SherlockFragment {
	
	private static final Logger logger = Logger.getLogger(MyGarage.class);
	
	private SharedPreferences sharedPreferences;

	private final String sensorType = "car";
	private String carFuelType;
	private String carModel;
	private String carManufacturer;
	private String carConstructionYear;

	private EditText carModelView;
	private EditText carManufacturerView;
	private EditText carConstructionYearView;
	
	private ScrollView garageForm;
	private LinearLayout garageProgress;
	
	private Spinner sensorSpinner;
	private ProgressBar sensorDlProgress;
	private Button sensorRetryButton;
	
	private JSONArray sensors;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		View view = inflater.inflate(R.layout.my_garage_layout, null);
		//TODO !fancy! search for sensors
		
		garageForm = (ScrollView) view.findViewById(R.id.garage_form);
		garageProgress = (LinearLayout) view.findViewById(R.id.addCarToGarage_status);
		
		carModelView = (EditText) view.findViewById(R.id.addCarToGarage_car_model);
		carManufacturerView = (EditText) view.findViewById(R.id.addCarToGarage_car_manufacturer);
		carConstructionYearView = (EditText) view.findViewById(R.id.addCarToGarage_car_constructionYear);

		TextWatcher textWatcher = new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				carModel = carModelView.getText().toString();
				carManufacturer = carManufacturerView.getText().toString();
				carConstructionYear = carConstructionYearView.getText()
						.toString();
			}

			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

		};

		carModelView.addTextChangedListener(textWatcher);
		carManufacturerView.addTextChangedListener(textWatcher);
		carConstructionYearView.addTextChangedListener(textWatcher);

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				carFuelType = getCorrectFuelTypeFromCheckbox(v.getId());
				logger.info(carFuelType);
			}
		};

		RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radiogroup_fueltype);
		carFuelType = getCorrectFuelTypeFromCheckbox( radioGroup.getCheckedRadioButtonId());

		RadioButton rbGasoline = (RadioButton) view.findViewById(R.id.radio_gasoline);
		rbGasoline.setOnClickListener(listener);
		RadioButton rbDiesel = (RadioButton) view.findViewById(R.id.radio_diesel);
		rbDiesel.setOnClickListener(listener);

		view.findViewById(R.id.register_car_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if(UserManager.instance().isLoggedIn()){
							if (carModel != null && carManufacturer != null
									&& carConstructionYear != null
									&& carFuelType != null) {
								registerSensorAtServer(sensorType, carManufacturer,
										carModel, carConstructionYear, carFuelType);
							}
						} else {
							LoginFragment loginFragment = new LoginFragment();
			                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, loginFragment, "LOGIN").addToBackStack(null).commit();
						}
					}
				});
		

		sensorSpinner = (Spinner) view.findViewById(R.id.dashboard_current_sensor_spinner);
		sensorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			private boolean firstSelect = true;
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, 
		            int pos, long id) {
				if(!firstSelect){
					logger.info(parent.getItemAtPosition(pos)+"");
					
					try {
						updateCurrentSensor(((JSONObject) parent.getItemAtPosition(pos)).getString("id"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("manufacturer"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("model"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("fuelType"),
								((JSONObject) parent.getItemAtPosition(pos)).getInt("constructionYear"));
			        	DashboardFragment dashboardFragment = new DashboardFragment();
			            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, dashboardFragment).commit();
					} catch (JSONException e) {
						logger.warn(e.getMessage(), e);
					}
				}else{
					firstSelect = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				//TODO do something, also nothing is selected if the same item as selected is selected
			}
		});
		sensorDlProgress = (ProgressBar) view.findViewById(R.id.sensor_dl_progress);
		sensorRetryButton = (Button) view.findViewById(R.id.retrybutton);
		sensorRetryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				downloadSensors();
			}
		});
		
		if(!UserManager.instance().isLoggedIn()){
			carManufacturerView.setEnabled(false);
			carConstructionYearView.setEnabled(false);
			carModelView.setEnabled(false);
			rbGasoline.setEnabled(false);
			rbDiesel.setEnabled(false);
			((Button) view.findViewById(R.id.register_car_button)).setText(R.string.action_sign_in_short);
			((TextView) view.findViewById(R.id.title_create_new_sensor)).setText(R.string.garage_not_signed_in);
		}

		downloadSensors();
		
		TypefaceEC.applyCustomFont((ViewGroup) view.findViewById(R.id.mygaragelayout), TypefaceEC.Raleway(getActivity()));
		
		return view;
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
		// TODO engine Displacement
		double engineDisplacement = 2.0;
		Car car = new Car(fuelType, carManufacturer, carModel, sensorid, year, engineDisplacement);
		CarManager.instance().setCat(car);
	}

	/**
	 * Get the fuel type form the checkbox
	 * @param resid
	 * @return
	 */
	private String getCorrectFuelTypeFromCheckbox(int resid){
		switch(resid){
		case R.id.radio_diesel:
			return "diesel";
		case R.id.radio_gasoline:
			return "gasoline";
		}
		return "none";
	}
	
	private void selectSensorFromSharedPreferences() throws JSONException{
		if(CarManager.instance().isCarSet()){
			String prefSensorid = CarManager.instance().getCar().getId();
			if(prefSensorid.equals("nosensor") == false){
				for(int i = 0; i<sensors.length(); i++){
					//iterate over sensors
					if(((JSONObject) sensors.get(i)).getString("id").equals(prefSensorid)){
						sensorSpinner.setSelection(i);
						logger.info(((JSONObject) sensors.get(i)).getString("id")+" "+prefSensorid);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Download sensors from the server
	 */
	private void downloadSensors(){
		sensorDlProgress.setVisibility(View.VISIBLE);
		sensorSpinner.setVisibility(View.GONE);
		sensorRetryButton.setVisibility(View.GONE);
		
		RestClient.downloadSensors(new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				sensorDlProgress.setVisibility(View.GONE);
				sensorRetryButton.setVisibility(View.VISIBLE);
				Crouton.makeText(getActivity(), getResources().getString(R.string.error_host_not_found), Style.ALERT).show();
			}
			
			@Override
			public void onSuccess(JSONObject response) {
				super.onSuccess(response);
				ArrayList<JSONObject> a = new ArrayList<JSONObject>();
				try {					
					JSONArray res = response.getJSONArray("sensors");
					for(int i = 0; i<res.length(); i++){
						if(((JSONObject) res.get(i)).optString("type", "none").equals("car")){
							a.add(((JSONObject) res.get(i)).getJSONObject("properties"));
						}
					}
					
		

				} catch (JSONException e) {
					logger.warn(e.getMessage(), e);
				} finally {
					sensors = new JSONArray(a);
					sensorSpinner.setAdapter(new SensorAdapter());
					sensorDlProgress.setVisibility(View.GONE);
					sensorSpinner.setVisibility(View.VISIBLE);
					try {
						selectSensorFromSharedPreferences();
					} catch (JSONException e) {
						logger.warn(e.getMessage(), e);
					}
				}
				
			}
		});
		
	}	

	/**
	 * Register a new sensor (car) at the server
	 * @param sensorType 
	 * @param carManufacturer Car manufacturer
	 * @param carModel Car model
	 * @param carConstructionYear Construction year of the car
	 * @param carFuelType Fuel type of the car
	 */
	private void registerSensorAtServer(final String sensorType,
			final String carManufacturer, final String carModel,
			final String carConstructionYear, final String carFuelType) {

		String sensorString = String
				.format("{ \"type\": \"%s\", \"properties\": {\"manufacturer\": \"%s\", \"model\": \"%s\", \"fuelType\": \"%s\", \"constructionYear\": %s } }",
						sensorType, carManufacturer, carModel, carFuelType,
						carConstructionYear);
		
		User user = UserManager.instance().getUser();
		String username = user.getUsername();
		String token = user.getToken();
		
		RestClient.createSensor(sensorString, username, token, new AsyncHttpResponseHandler(){
			
			@Override
			public void onStart() {
				super.onStart();
				showProgress(true);
			}

			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				if(content.equals("can't resolve host") ){
					Crouton.makeText(getActivity(), getResources().getString(R.string.error_host_not_found), Style.ALERT).show();
				}
				showProgress(false);
			}
			
			
			@Override
			public void onSuccess(int httpStatusCode, Header[] h, String response) {
				super.onSuccess(httpStatusCode, h, response);
				String location = "";
				for (int i = 0; i< h.length; i++){
					if( h[i].getName().equals("Location")){
						location += h[i].getValue();
						break;
					}
				}
				logger.info(httpStatusCode+" "+location);
				
				String sensorId = location.substring(location.lastIndexOf("/")+1, location.length());
				//put the sensor id into shared preferences
				// TODO set EngineDisplacement
				double engineDisplacement = 2.0;
				int year = Integer.parseInt(carConstructionYear);
				Car car = new Car(carFuelType, carManufacturer, carModel, sensorId, year, engineDisplacement);
				CarManager.instance().setCat(car);
				//go back to the dashboard
				// TODO use existing dashboard, dont create a new!
	        	DashboardFragment dashboardFragment = new DashboardFragment();
	            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, dashboardFragment).commit();
			}
		});

	}
	
	/**
	 * Shows the progress UI and hides the register form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			garageProgress.setVisibility(View.VISIBLE);
			garageProgress.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							garageProgress
									.setVisibility(show ? View.VISIBLE
											: View.GONE);
						}
					});

			garageForm.setVisibility(View.VISIBLE);
			garageForm.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							garageForm.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			garageProgress.setVisibility(show ? View.VISIBLE : View.GONE);
			garageForm.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	private class SensorAdapter extends BaseAdapter implements SpinnerAdapter {

        @Override
        public int getCount() {
            return sensors.length();
        }

        @Override
        public Object getItem(int position) {
            try {
				return ((JSONObject) sensors.get(position));
			} catch (JSONException e) {
				logger.warn(e.getMessage(), e);
			}
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        boolean firstTime = true;
        
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TextView text = new TextView(getActivity());
            if(firstTime && !CarManager.instance().isCarSet()){
            	text.setText(getResources().getString(R.string.please_select));
            	firstTime = false;
            } else {
	            try {
					text.setText(
							((JSONObject) getItem(position)).getString("manufacturer")+" "+
							((JSONObject) getItem(position)).getString("model")+" ("+
							((JSONObject) getItem(position)).getString("fuelType")+" "+
							((JSONObject) getItem(position)).getInt("constructionYear")+")");
				} catch (JSONException e) {
					text.setText("error");
					logger.warn(e.getMessage(), e);
				}
            }
            return text;
        }

    }		
}
