package car.io.adapter;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import car.io.exception.LocationInvalidException;

import android.text.TextUtils;
import android.util.Log;

public class UploadManager {

	private static final String TAG = "obd2";

	// TODO Configure Url in property document/shared preferences
	private String url = "http://giv-car.uni-muenster.de:8080/stable/rest/users/upload/tracks";

	private DbAdapter dbAdapter;

	public UploadManager(DbAdapter dbAdapter) {
		this.dbAdapter = dbAdapter;
	}

	/**
	 * This methods uploads all local tracks to the server
	 */
	public void uploadAllTracks() {

		/*
		 * This is just for testing
		 */
		Track dummyTrack = new Track("VIN", "Diesel", dbAdapter);
		dummyTrack.setDescription("This is a description of the track.");
		dummyTrack.setName("This is the Name of the track");

		try {
			Measurement dummyMeasurement = new Measurement(12.365f, 24.068f);
			dummyMeasurement.setSpeed(220);
			dummyTrack.addMeasurement(dummyMeasurement);

			Measurement dummyMeasurement2 = new Measurement(55.365f, 7.068f);
			dummyMeasurement2.setSpeed(160);
			dummyTrack.addMeasurement(dummyMeasurement2);

			Log.i(TAG, "Measurement object created.");
		} catch (LocationInvalidException e1) {
			Log.e(TAG, "Measurement object creation failed.");
			e1.printStackTrace();
		}

		ArrayList<Track> trackList = new ArrayList<Track>();
		trackList.add(dummyTrack);
		/*
		 * This is where testing ends. Remember to correctly comment in or out
		 * the next line as well.
		 */

		// ArrayList<Track> trackList = dbAdapter.getAllTracks();
		if (trackList.size() == 0) {
			Log.d(TAG, "No stored tracks in local db found.");
			return;
		}

		ArrayList<String> trackJsonList = new ArrayList<String>();

		for (Track track : trackList) {
			trackJsonList.add(createTrackJson(track));
		}

		Log.i("Size", String.valueOf(trackJsonList.size()));

		for (String trackJsonString : trackJsonList) {
			JSONObject obj = null;

			try {
				obj = new JSONObject(trackJsonString);
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing measurement string to JSON object.");
				e.printStackTrace();
			}

			// TODO Configure X-User, X-Token in property document/shared
			// preferences
			sendHttpPost(url, obj, "upload", "upload");
		}
	}

	/**
	 * Converts Track Object into track.create.json string
	 * 
	 * TODO Outsource JSON converting to Converter class
	 * 
	 * @return
	 */
	private String createTrackJson(Track track) {

		String trackName = track.getName();
		String trackDescription = track.getDescription();

		// TODO configure sensorName in Track Class.
		// TODO Error Handling: only registered sensor names are accepted from
		// server side
		String trackSensorName = "testsensor1";

		String trackElementJson = String
				.format("{ \"type\": \"FeatureCollection\", \"properties\": { \"name\": \"%s\", \"description\": \"%s\", \"sensor\": \"%s\" }, \"features\": [",
						trackName, trackDescription, trackSensorName);

		ArrayList<Measurement> measurements = track.getMeasurements();
		ArrayList<String> measurementElements = new ArrayList<String>();

		for (Measurement measurement : measurements) {
			String lat = String.valueOf(measurement.getLatitude());
			String lon = String.valueOf(measurement.getLongitude());
			// TODO Format change a la 2013-05-16T02:13:27Z needed
			String time = String.valueOf(measurement.getMeasurementTime());
			String sensorNameMeasurement = "testsensor1";
			String speed = String.valueOf(measurement.getSpeed());
			String measurementJson = String
					.format("{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %s, %s ] }, \"properties\": { \"time\": \"2013-05-16T02:13:27Z\", \"sensor\": { \"name\": \"testsensor1\"}, \"phenomenons\": { \"testphenomenon1\": { \"value\": %s } } } }",
							lat, lon, speed);
			measurementElements.add(measurementJson);
		}

		String measurementElementsJson = TextUtils.join(",",
				measurementElements);
		Log.d("measurementElem", measurementElementsJson);
		String closingElementJson = "]}";

		String trackString = String.format("%s %s %s", trackElementJson,
				measurementElementsJson, closingElementJson);
		Log.d("Track", trackString);

		return trackString;
	}

	/**
	 * Uploads the json object to the server
	 * 
	 * @param url
	 *            Url
	 * @param jsonObjSend
	 *            The Json Object
	 * @param xToken
	 *            Token
	 * @param xUser
	 *            Username
	 */
	private void sendHttpPost(String url, JSONObject jsonObjSend,
			String xToken, String xUser) {

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPostRequest = new HttpPost(url);

			StringEntity se = new StringEntity(jsonObjSend.toString());
			Log.d(TAG + "SE", jsonObjSend.toString());

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("X-Token", xToken);
			httpPostRequest.setHeader("X-User", xUser);

			long timeSubtrahend = System.currentTimeMillis();
			HttpResponse response = (HttpResponse) httpclient
					.execute(httpPostRequest);
			Log.i(TAG,
					String.format("HTTP response time: [%s ms]",
							(System.currentTimeMillis() - timeSubtrahend)));
			String statusCode = String.valueOf(response.getStatusLine()
					.getStatusCode());
			String reasonPhrase = response.getStatusLine().getReasonPhrase();
			Log.d(TAG, String.format("%s: %s", statusCode, reasonPhrase));

			if (statusCode != "xyz") { // TODO replace with 201
				CharSequence text = String.format("%s: %s", statusCode,
						reasonPhrase);
				Log.i(TAG, text + "");
			}

		} catch (Exception e) {
			Log.e(TAG, "Error occured while sending JSON file to server.");
			e.printStackTrace();
		}
	}

}
