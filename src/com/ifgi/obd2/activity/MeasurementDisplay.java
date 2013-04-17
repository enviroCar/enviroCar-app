package com.ifgi.obd2.activity;

import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.ifgi.obd2.R;
import com.ifgi.obd2.adapter.DbAdapter;
import com.ifgi.obd2.adapter.DbAdapterLocal;
import com.ifgi.obd2.adapter.Measurement;

/**
 * Activity that displays one measurement and the measurement data on the
 * screen. The id of the desired measurement has to be put in the bundle that is
 * created before the activity is called...
 * 
 * @author jakob
 * 
 */

public class MeasurementDisplay extends Activity {

	// measurement properties

	private TextView idTextView;
	private TextView latitudeTextView;
	private TextView longitudeTextView;
	private TextView measurementTimeTextView;
	private TextView throttlePositionTextView;
	private TextView rpmTextView;
	private TextView speedTextView;
	private TextView fuelTypeTextView;
	private TextView engineLoadTextView;
	private TextView fuelConsumptionTextView;
	private TextView intakePressureTextView;
	private TextView intakeTemperatureTextView;
	private TextView shortTermTrimBank1TextView;
	private TextView longTermTrimBank1TextView;
	private TextView mafTextView;
	private TextView carTextView;

	// The measurement

	Measurement measurement;

	// Db Adapter for the SQLite Database connection

	private DbAdapter dbAdapter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.measurement_display);

		// Determine the id by going through the bundle

		Bundle extras = getIntent().getExtras();
		String id = extras.getString("id");

		// Get the measurement from the SQLite DB

		initDbAdapter();
		measurement = dbAdapter.getMeasurement(Integer.valueOf(id));
		dbAdapter.close();

		// Fill all the Text Views

		if (measurement != null) {
			fillTextViews();
		}

	}

	/**
	 * Helper method that puts the properties of the measurement in the text
	 * views
	 */
	private void fillTextViews() {

		// Find Textviews

		idTextView = (TextView) findViewById(R.id.idTextView);
		latitudeTextView = (TextView) findViewById(R.id.latitudeTextDisplay);
		longitudeTextView = (TextView) findViewById(R.id.longitudeTextDisplay);
		measurementTimeTextView = (TextView) findViewById(R.id.measurementTimeTextDisplay);
		throttlePositionTextView = (TextView) findViewById(R.id.throttlePositionTextDisplay);
		rpmTextView = (TextView) findViewById(R.id.rpmTextDisplay);
		speedTextView = (TextView) findViewById(R.id.speedTextDisplay);
		fuelTypeTextView = (TextView) findViewById(R.id.fuelTypeTextDisplay);
		engineLoadTextView = (TextView) findViewById(R.id.engineLoadTextDisplay);
		fuelConsumptionTextView = (TextView) findViewById(R.id.fuelConsumptionTextDisplay);
		intakePressureTextView = (TextView) findViewById(R.id.intakePressureTextDisplay);
		intakeTemperatureTextView = (TextView) findViewById(R.id.intakeTemperatureTextDisplay);
		shortTermTrimBank1TextView = (TextView) findViewById(R.id.shortTermTrimBank1TextDisplay);
		longTermTrimBank1TextView = (TextView) findViewById(R.id.longTermTrimBank1TextDisplay);
		mafTextView = (TextView) findViewById(R.id.mafTextDisplay);
		carTextView = (TextView) findViewById(R.id.carTextDisplay);

		// Set properties

		idTextView.setText("ID: " + measurement.getId());
		latitudeTextView.setText("Latitude: " + measurement.getLatitude());
		longitudeTextView.setText("Longitude: " + measurement.getLongitude());
		Date date = new Date(measurement.getMeasurementTime());
		measurementTimeTextView.setText("Time: " + date.toString());
		throttlePositionTextView.setText("Throttle Position: "
				+ measurement.getThrottlePosition() + "%");
		rpmTextView.setText("RPM: " + measurement.getRpm());
		speedTextView.setText("Speed: " + measurement.getSpeed() + "km/h");
		fuelTypeTextView.setText("Fuel Type: " + measurement.getFuelType());
		engineLoadTextView.setText("Engine Load: "
				+ measurement.getEngineLoad() + " %");
		fuelConsumptionTextView.setText("Fuel Consumption: "
				+ measurement.getFuelConsumption() + " l/h");
		intakePressureTextView.setText("Intake Pressure: "
				+ measurement.getIntakePressure() + " kPa");
		intakeTemperatureTextView.setText("Intake Temperature: "
				+ measurement.getIntakeTemperature() + "°C");
		shortTermTrimBank1TextView.setText("Short Term Trim Bank 1: "
				+ measurement.getShortTermTrimBank1() + " %");
		longTermTrimBank1TextView.setText("Long Term Trim Bank 1: "
				+ measurement.getLongTermTrimBank1() + " %");
		mafTextView.setText("MAF: " + measurement.getMaf() + " g/s");
		carTextView.setText(measurement.getCar());

	}

	/**
	 * Init the DbAdapter
	 */
	private void initDbAdapter() {
		dbAdapter = new DbAdapterLocal(getApplicationContext());
		dbAdapter.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

}
