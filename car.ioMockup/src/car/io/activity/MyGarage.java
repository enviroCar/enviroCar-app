package car.io.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import car.io.R;
import car.io.adapter.UploadManager;
import car.io.application.ECApplication;
import car.io.views.TYPEFACE;
import android.widget.RadioButton;

import com.actionbarsherlock.app.SherlockFragment;

public class MyGarage extends SherlockFragment {

	// TODO get url, token, username from sharedprefs
	private String url = "http://giv-car.uni-muenster.de:8080/stable/rest/sensors";
	
	String username = ((ECApplication) getActivity().getApplication()).getUser().getUsername();
	String token = ((ECApplication) getActivity().getApplication()).getUser().getToken();

	private static final String TAG = "MyGarage";

	private final String sensorType = "car";
	private String carFuelType;
	private String carModel;
	private String carManufacturer;
	private String carConstructionYear;

	private EditText carModelView;
	private EditText carManufacturerView;
	private EditText carConstructionYearView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.my_garage_layout, null);
		carModelView = (EditText) view
				.findViewById(R.id.addCarToGarage_car_model);
		carManufacturerView = (EditText) view
				.findViewById(R.id.addCarToGarage_car_manufacturer);
		carConstructionYearView = (EditText) view
				.findViewById(R.id.addCarToGarage_car_constructionYear);

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
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

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

		RadioGroup radioGroup = (RadioGroup) view
				.findViewById(R.id.radiogroup_fueltype);
		int selected = radioGroup.getCheckedRadioButtonId();
		RadioButton checked = (RadioButton) view.findViewById(selected);
		carFuelType = setFuelType(checked.getText().toString());

		RadioButton rbGasoline = (RadioButton) view
				.findViewById(R.id.radio_gasoline);
		rbGasoline.setOnClickListener(listener);
		RadioButton rbDiesel = (RadioButton) view
				.findViewById(R.id.radio_diesel);
		rbDiesel.setOnClickListener(listener);
		RadioButton rbElectric = (RadioButton) view
				.findViewById(R.id.radio_electric);
		rbElectric.setOnClickListener(listener);

		view.findViewById(R.id.register_car_button).setOnClickListener(
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

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		TYPEFACE.applyCustomFont((ViewGroup) view,
				TYPEFACE.Raleway(getActivity()));
	}

	private void registerSensorAtServer(String sensorType,
			String carManufacturer, String carModel,
			String carConstructionYear, String carFuelType) {

		String sensorString = String
				.format("{ \"type\": \"%s\", \"properties\": {\"manufacturer\": \"%s\", \"model\": \"%s\", \"fuelType\": \"%s\", \"constructionYear\": %s } }",
						sensorType, carManufacturer, carModel, carFuelType,
						carConstructionYear);

		try {
			JSONObject obj = new JSONObject(sensorString);
			UploadManager uploadManager = new UploadManager();
			uploadManager.sendHttpPost(url, obj, token, username);
		} catch (JSONException e) {
			Log.e("TAG",
					"Error while creating JSON string for sensor registration.");
			e.printStackTrace();
		}
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
}
