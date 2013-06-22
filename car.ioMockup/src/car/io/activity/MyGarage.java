package car.io.activity;

import org.apache.http.Header;
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
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import car.io.R;
import car.io.application.ECApplication;
import car.io.application.RestClient;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class MyGarage extends SherlockActivity {

	private String url = "http://giv-car.uni-muenster.de:8080/stable/rest/sensors";
	
	private SharedPreferences sharedPreferences;

	private static final String TAG = "MyGarage";

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
	
	private int actionBarTitleID = 0;
	
	private JSONArray sensors;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_garage_layout);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		final ActionBar actionBar = getSupportActionBar();
		
		//TODO implement something that can detect from where the activity has been called
		//to allow for things like
		// * disable home as up
		// * display message
		//TODO really implement this to detect if a list of available sensors should be displayed
		//TODO !fancy! search for sensors
		
		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}

		actionBar.setLogo(getResources().getDrawable(R.drawable.home_icon));

		actionBar.setDisplayHomeAsUpEnabled(true);
		
		garageForm = (ScrollView) this.findViewById(R.id.garage_form);
		garageProgress = (LinearLayout) this.findViewById(R.id.addCarToGarage_status);
		
		carModelView = (EditText) findViewById(R.id.addCarToGarage_car_model);
		carManufacturerView = (EditText) findViewById(R.id.addCarToGarage_car_manufacturer);
		carConstructionYearView = (EditText) findViewById(R.id.addCarToGarage_car_constructionYear);

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
				RadioButton rb = (RadioButton) v;
				carFuelType = rb.getText().toString().toLowerCase();
				Log.e(TAG, carFuelType);
			}
		};

		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radiogroup_fueltype);
		int selected = radioGroup.getCheckedRadioButtonId();
		RadioButton checked = (RadioButton) findViewById(selected);
		carFuelType = checked.getText().toString().toLowerCase();

		RadioButton rbGasoline = (RadioButton) findViewById(R.id.radio_gasoline);
		rbGasoline.setOnClickListener(listener);
		RadioButton rbDiesel = (RadioButton) findViewById(R.id.radio_diesel);
		rbDiesel.setOnClickListener(listener);
		RadioButton rbElectric = (RadioButton) findViewById(R.id.radio_electric);
		rbElectric.setOnClickListener(listener);

		findViewById(R.id.register_car_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Log.d(TAG, carModel + " " + carManufacturer + " "
								+ carFuelType);
						if (carModel != null && carManufacturer != null
								&& carConstructionYear != null
								&& carFuelType != null) {
							registerSensorAtServer(sensorType, carManufacturer,
									carModel, carConstructionYear, carFuelType);
						}
					}
				});
		

		sensorSpinner = (Spinner) findViewById(R.id.dashboard_current_sensor_spinner);
		sensorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			private boolean firstSelect = true;
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, 
		            int pos, long id) {
				if(!firstSelect){
					Log.i("item",parent.getItemAtPosition(pos)+"");
					
					try {
						((ECApplication) getApplication()).updateCurrentSensor(((JSONObject) parent.getItemAtPosition(pos)).getString("id"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("manufacturer"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("model"),
								((JSONObject) parent.getItemAtPosition(pos)).getString("fuelType"),
								((JSONObject) parent.getItemAtPosition(pos)).getInt("constructionYear"));
						setResult(DashboardFragment.SENSOR_CHANGED_RESULT);
						finish();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					firstSelect = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				//TODO do something
			}
		});
		sensorDlProgress = (ProgressBar) findViewById(R.id.sensor_dl_progress);
		sensorRetryButton = (Button) findViewById(R.id.retrybutton);
		sensorRetryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				dlSensors();
			}
		});
		
		dlSensors();
		
		
		TYPEFACE.applyCustomFont((ViewGroup) findViewById(R.id.mygaragelayout), TYPEFACE.Raleway(this));
	}
	
	private void selectSensorFromSharedPreferences() throws JSONException{
		if(PreferenceManager.getDefaultSharedPreferences(getApplication()).contains(ECApplication.PREF_KEY_SENSOR_ID)){
			String prefSensorid = PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(ECApplication.PREF_KEY_SENSOR_ID, "nosensor");
			if(prefSensorid.equals("nosensor") == false){
				for(int i = 0; i<sensors.length(); i++){
					//iterate over sensors
					if(((JSONObject) sensors.get(i)).getJSONObject("properties").getString("id").equals(prefSensorid)){
						sensorSpinner.setSelection(i);
						Log.i("setspinner from prefs",((JSONObject) sensors.get(i)).getJSONObject("properties").getString("id")+" "+prefSensorid);
						break;
					}
				}
			}
		}
	}
	
	
	private void dlSensors(){
		sensorDlProgress.setVisibility(View.VISIBLE);
		sensorSpinner.setVisibility(View.GONE);
		sensorRetryButton.setVisibility(View.GONE);
		
		RestClient.downloadSensors(new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				sensorDlProgress.setVisibility(View.GONE);
				sensorRetryButton.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onSuccess(JSONObject response) {
				super.onSuccess(response);
				try {
					sensors = response.getJSONArray("sensors");
					sensorSpinner.setAdapter(new SensorAdapter());
					sensorDlProgress.setVisibility(View.GONE);
					sensorSpinner.setVisibility(View.VISIBLE);
					selectSensorFromSharedPreferences();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}	

	private void registerSensorAtServer(String sensorType,
			String carManufacturer, String carModel,
			String carConstructionYear, String carFuelType) {

		String sensorString = String
				.format("{ \"type\": \"%s\", \"properties\": {\"manufacturer\": \"%s\", \"model\": \"%s\", \"fuelType\": \"%s\", \"constructionYear\": %s } }",
						sensorType, carManufacturer, carModel, carFuelType,
						carConstructionYear);
		
		Editor edit = sharedPreferences.edit();
		edit.putString(ECApplication.PREF_KEY_FUEL_TYPE, carFuelType);
		edit.putString(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR, carConstructionYear);
		edit.putString(ECApplication.PREF_KEY_CAR_MANUFACTURER, carManufacturer);
		edit.putString(ECApplication.PREF_KEY_CAR_MODEL, carModel);
		edit.commit();
		
		String username =((ECApplication) getApplication()).getUser().getUsername();
		String token = ((ECApplication) getApplication()).getUser().getToken();
		
		RestClient.createSensor(sensorString, username, token, new AsyncHttpResponseHandler(){
			
			@Override
			public void onStart() {
				super.onStart();
				showProgress(true);
			}

			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
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
				Log.i("create sensor", httpStatusCode+" "+location);
				
				String sensorId = location.substring(location.lastIndexOf("/")+1, location.length());
				//put the sensor id into shared preferences
				Editor edit = sharedPreferences.edit();
				edit.putString(ECApplication.PREF_KEY_SENSOR_ID, sensorId);
				edit.commit();
				setResult(httpStatusCode);
				finish();
			}
		});

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
				return ((JSONObject) sensors.get(position)).getJSONObject("properties");
			} catch (JSONException e) {
				e.printStackTrace();
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
            TextView text = new TextView(MyGarage.this);
            if(firstTime && !PreferenceManager.getDefaultSharedPreferences(getApplication()).contains(ECApplication.PREF_KEY_SENSOR_ID)){
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
					e.printStackTrace();
				}
            }
            return text;
        }

    }		
}
