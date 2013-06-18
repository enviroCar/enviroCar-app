package car.io.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.UploadManager;
import car.io.application.ECApplication;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

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
	
	private int actionBarTitleID = 0;
	
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
		
		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}

		actionBar.setLogo(getResources().getDrawable(R.drawable.home_icon));
		
		
		actionBar.setDisplayHomeAsUpEnabled(true);		
		
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
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

		};

		carModelView.addTextChangedListener(textWatcher);
		carManufacturerView.addTextChangedListener(textWatcher);
		carConstructionYearView.addTextChangedListener(textWatcher);

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				RadioButton rb = (RadioButton) v;
				setFuelType(rb.getText().toString());
				Log.e(TAG, carFuelType);
			}
		};

		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radiogroup_fueltype);
		int selected = radioGroup.getCheckedRadioButtonId();
		RadioButton checked = (RadioButton) findViewById(selected);
		carFuelType = setFuelType(checked.getText().toString());

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
		TYPEFACE.applyCustomFont((ViewGroup) findViewById(R.id.mygaragelayout), TYPEFACE.Raleway(this));
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
		//TODO Sensor id

		try {
			JSONObject obj = new JSONObject(sensorString);
			String username =((ECApplication) getApplication()).getUser().getUsername();
			String token = ((ECApplication) getApplication()).getUser().getToken();
			UploadManager uploadManager = new UploadManager();
			uploadManager.sendHttpPost(url, obj, token, username);
		} catch (JSONException e) {
			Log.e("TAG",
					"Error while creating JSON string for sensor registration.");
			e.printStackTrace();
		}
		finish();
	}

	private String setFuelType(String fuelType) {
		// TODO Eliminate variations in spelling of "Gasoline" or "Gasolene"
		// between app and server

		if (fuelType.equals("Gasoline")) {
			carFuelType = "gasolene";
		}
		if (fuelType.equals("Diesel")) {
			carFuelType = "diesel";
		}
		if (fuelType.equals("Electric")) {
			carFuelType = "electric";
		}

		return carFuelType;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}	
}
