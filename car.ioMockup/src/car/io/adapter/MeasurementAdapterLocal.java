package car.io.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * Implementation of the MeasurementAdapter interface.
 * 
 * @author jakob
 * 
 */

public class MeasurementAdapterLocal implements MeasurementAdapter {

	// URL of the server's php script

	private final String URI = "http://giv-moellers.uni-muenster.de/uploadMeasurement.php";

	@Override
	public void uploadMeasurement(Measurement measurement)
			throws ClientProtocolException, IOException {

		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URI);

		// Add data
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

		// this will be $_POST arguments in php
		nameValuePairs.add(new BasicNameValuePair("latitude", String
				.valueOf(measurement.getLatitude())));
		nameValuePairs.add(new BasicNameValuePair("longitude", String
				.valueOf(measurement.getLongitude())));
		nameValuePairs.add(new BasicNameValuePair("measurement_time", String
				.valueOf(measurement.getMeasurementTime())));
		// nameValuePairs.add(new BasicNameValuePair("throttle_position", String
		// .valueOf(measurement.getThrottlePosition())));
		nameValuePairs.add(new BasicNameValuePair("speed", String
				.valueOf(measurement.getSpeed())));
		// nameValuePairs.add(new BasicNameValuePair("fuel_type", measurement
		// .getFuelType()));
		// nameValuePairs.add(new BasicNameValuePair("engine_load", String
		// .valueOf(measurement.getEngineLoad())));
		// nameValuePairs.add(new BasicNameValuePair("fuel_consumption", String
		// .valueOf(measurement.getFuelConsumption())));
		// nameValuePairs.add(new BasicNameValuePair("intake_pressure", String
		// .valueOf(measurement.getIntakePressure())));
		// nameValuePairs.add(new BasicNameValuePair("intake_temperature",
		// String
		// .valueOf(measurement.getIntakeTemperature())));
		// nameValuePairs.add(new BasicNameValuePair("short_term_trim_bank_1",
		// String.valueOf(measurement.getShortTermTrimBank1())));
		// nameValuePairs.add(new BasicNameValuePair("long_term_trim_bank_1",
		// String.valueOf(measurement.getLongTermTrimBank1())));
		nameValuePairs.add(new BasicNameValuePair("maf", String
				.valueOf(measurement.getMaf())));
		// nameValuePairs.add(new BasicNameValuePair("car",
		// measurement.getCar()));

		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		// Execute HTTP Post Request
		HttpResponse response = httpclient.execute(httppost);

		// this is the response
		HttpEntity entity = response.getEntity();
		String id = EntityUtils.toString(entity);
		// output for debugging...
		Log.i("obd2", "id of new row: " + id);

	}

}
