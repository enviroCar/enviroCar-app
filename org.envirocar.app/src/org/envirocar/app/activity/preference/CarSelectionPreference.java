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
package org.envirocar.app.activity.preference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.network.RestClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class CarSelectionPreference extends DialogPreference {

	private static final Logger logger = Logger.getLogger(CarSelectionPreference.class);
	private static final String DEFAULT_VALUE = "null";
	private Car car;
	private LinearLayout garageProgress;
	private EditText carModelView;
	private EditText carManufacturerView;
	private EditText carConstructionYearView;
	protected String carModel;
	protected String carManufacturer;
	protected String carConstructionYear;
	protected String carFuelType;
	private Spinner sensorSpinner;
	private ProgressBar sensorDlProgress;
	private Button sensorRetryButton;
	private final String sensorType = "car";
	protected JSONArray sensors;
	private ScrollView garageForm;
	
	public CarSelectionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
        
        setDialogLayoutResource(R.layout.my_garage_layout);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
	}
	
	@Override
	protected void onBindDialogView(View view) {
		setupUIItems(view);
	}
	
	private void setupUIItems(View view) {
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
							else {
								Toast.makeText(getDialog().getContext(),
										"Please log in", Toast.LENGTH_SHORT).show();
							}
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
			int shortAnimTime = getContext().getResources().getInteger(
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
					Toast.makeText(getContext(),
							getContext().getString(R.string.error_host_not_found), Toast.LENGTH_SHORT).show();
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
				car = new Car(carFuelType, carManufacturer, carModel, sensorId, year, engineDisplacement);
			}
		});

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

	protected void downloadSensors() {
		sensorDlProgress.setVisibility(View.VISIBLE);
		sensorSpinner.setVisibility(View.GONE);
		sensorRetryButton.setVisibility(View.GONE);
		
		RestClient.downloadSensors(new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				sensorDlProgress.setVisibility(View.GONE);
				sensorRetryButton.setVisibility(View.VISIBLE);
				Toast.makeText(getContext(),
						getContext().getString(R.string.error_host_not_found), Toast.LENGTH_SHORT).show();
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
				}
				
			}
		});
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
		car = new Car(fuelType, carManufacturer, carModel, sensorid, year, engineDisplacement);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
	        car = new Car(FuelType.GASOLINE, "test0r", "fast0r", "1234", 2001, 1.45);
	        persistString(serializeCar());
	        //TODO commit somehow  - listeners do not get informed
		}
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	    if (restorePersistedValue) {

	    	car = instantiateCar(this.getPersistedString(DEFAULT_VALUE));
	    } else {
	        // Set default state from the XML attribute
	        car = instantiateCar((String) defaultValue); 
	        persistString(serializeCar());
	    }
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
	    final Parcelable superState = super.onSaveInstanceState();
	    // Check whether this Preference is persistent (continually saved)
	    if (isPersistent()) {
	        // No need to save instance state since it's persistent, use superclass state
	        return superState;
	    }

	    // Create instance of custom BaseSavedState
	    final SavedState myState = new SavedState(superState);
	    // Set the state's value with the class member that holds current setting value
	    myState.car = car;
	    return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
	    // Check whether we saved the state in onSaveInstanceState
	    if (state == null || !state.getClass().equals(SavedState.class)) {
	        // Didn't save the state, so call superclass
	        super.onRestoreInstanceState(state);
	        return;
	    }

	    // Cast state to custom BaseSavedState and pass to superclass
	    SavedState myState = (SavedState) state;
	    super.onRestoreInstanceState(myState.getSuperState());
	    
	}

	private String serializeCar() {
		ObjectOutputStream oos = null;
		Base64OutputStream b64 = null;
		try {
			ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(byteArrayOut);
			oos.writeObject(car);
			oos.flush();
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
	        b64 = new Base64OutputStream(out, Base64.DEFAULT);
	        b64.write(byteArrayOut.toByteArray());
	        b64.flush();
	        b64.close();
	        out.flush();
	        out.close();
			
	        String result = new String(out.toByteArray());
			return result;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		} finally {
			if (oos != null)
				try {
					b64.close();
					oos.close();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
		}
		return null;
	}
	
	public static Car instantiateCar(String object) {
		ObjectInputStream ois = null;
		try {
			Base64InputStream b64 = new Base64InputStream(new ByteArrayInputStream(object.getBytes()), Base64.DEFAULT);
			ois = new ObjectInputStream(b64);
			Car car = (Car) ois.readObject();
			return car;
		} catch (StreamCorruptedException e) {
			logger.warn(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			logger.warn(e.getMessage(), e);
		} finally {
			if (ois != null)
				try {
					ois.close();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
		}
		return null;
	}

	public static class SavedState extends BaseSavedState {
	    // Member that holds the setting's value
	    // Change this data type to match the type saved by your Preference
	    Car car;

	    public SavedState(Parcelable superState) {
	        super(superState);
	    }

	    public SavedState(Parcel source) {
	        super(source);
	        // Get the current preference's value
	        car = (Car) source.readSerializable();  // Change this to read the appropriate data type
	    }

	    @Override
	    public void writeToParcel(Parcel dest, int flags) {
	        super.writeToParcel(dest, flags);
	        // Write the preference's value
	        dest.writeSerializable(car);  // Change this to write the appropriate data type
	    }

	    // Standard creator object using an instance of this class
	    public static final Parcelable.Creator<SavedState> CREATOR =
	            new Parcelable.Creator<SavedState>() {

	        public SavedState createFromParcel(Parcel in) {
	            return new SavedState(in);
	        }

	        public SavedState[] newArray(int size) {
	            return new SavedState[size];
	        }
	    };
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
            TextView text = new TextView(parent.getContext());
            if(firstTime && CarManager.instance().getCar() == null){
            	text.setText(getContext().getString(R.string.please_select));
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
