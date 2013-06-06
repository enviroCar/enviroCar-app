package car.io.adapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import car.io.exception.FuelConsumptionException;

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

		// Track dummyTrack = new Track("VIN", "Diesel", dbAdapter);
		// dummyTrack.setDescription("This is a description of the track.");
		// dummyTrack.setName("This is the Name of the track");
		// dummyTrack.setFuelType("Diesel");
		//
		// try {
		// Measurement dummyMeasurement = new Measurement(12.365f, 24.068f);
		// dummyMeasurement.setMaf(456);
		// dummyMeasurement.setSpeed(220);
		// dummyTrack.addMeasurement(dummyMeasurement);
		// dummyMeasurement.setId(0);
		//
		// Measurement dummyMeasurement2 = new Measurement(55.365f, 7.068f);
		// dummyMeasurement2.setMaf(550);
		// dummyMeasurement2.setSpeed(130);
		// dummyTrack.addMeasurement(dummyMeasurement2);
		// dummyMeasurement2.setId(1);
		//
		// Log.i(TAG, "Measurement object created.");
		// } catch (LocationInvalidException e1) {
		// Log.e(TAG, "Measurement object creation failed.");
		// e1.printStackTrace();
		// }
		//
		// dummyTrack.commitTrackToDatabase();
		// ArrayList<Track> trackList = new ArrayList<Track>();
		// trackList.add(dummyTrack);

		/*
		 * This is where testing ends. Remember to correctly comment in or out
		 * the next line as well.
		 */

		cleanDumpFile();

		ArrayList<Track> trackList = dbAdapter.getAllTracks();

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
				savetoSdCard(obj);
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing measurement string to JSON object.");
				e.printStackTrace();
			}

			// TODO Configure X-User, X-Token in property document/shared
			// preferences
			int statusCode = sendHttpPost(url, obj, "upload", "upload");

			if (statusCode != -1 && statusCode == 201) {
				// TODO remove tracks from local storage if upload was
				// successful
				// TODO method dbAdapter.removeTrackFromLocalDb(Track) needed
			}
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
		String trackSensorName = "Car";

		String trackElementJson = String
				.format("{ \"type\": \"FeatureCollection\", \"properties\": { \"name\": \"%s\", \"description\": \"%s\", \"sensor\": \"%s\" }, \"features\": [",
						trackName, trackDescription, trackSensorName);

		ArrayList<Measurement> measurements = track.getMeasurements();
		ArrayList<String> measurementElements = new ArrayList<String>();

		for (Measurement measurement : measurements) {
			String lat = String.valueOf(measurement.getLatitude());
			String lon = String.valueOf(measurement.getLongitude());
			DateFormat dateFormat1 = new SimpleDateFormat("y-MM-d");
			DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");
			dateFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time = dateFormat1.format(measurement.getMeasurementTime())
					+ "T"
					+ dateFormat2.format(measurement.getMeasurementTime())
					+ "Z";
			String co2 = "0", consumption = "0";
			try {
				co2 = String.valueOf(track
						.getCO2EmissionOfMeasurement(measurement.getId()));
				consumption = String.valueOf(track
						.getFuelConsumptionOfMeasurement(measurement.getId()));
			} catch (FuelConsumptionException e) {
				e.printStackTrace();
			}

			String maf = String.valueOf(measurement.getMaf());
			String speed = String.valueOf(measurement.getSpeed());
			String measurementJson = String
					.format("{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %s, %s ] }, \"properties\": { \"time\": \"%s\", \"sensor\": { \"name\": \"%s\" }, \"phenomenons\": { \"MAF\": { \"value\": %s }, \"CO2\": { \"value\": %s }, \"Consumption\": { \"value\": %s }, \"Speed\": { \"value\": %s } } } }",
							lon, lat, time, trackSensorName, maf, co2,
							consumption, speed);
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
	 * @return Server response status code
	 */
	private int sendHttpPost(String url, JSONObject jsonObjSend, String xToken,
			String xUser) {

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
				String text = String.format("%s: %s", statusCode, reasonPhrase);
				Log.i(TAG, text);
			}

			return Integer.parseInt(statusCode);

		} catch (Exception e) {
			Log.e(TAG, "Error occured while sending JSON file to server.");
			e.printStackTrace();
			return -1;
		}

	}

	/**
	 * Deletes the dump file
	 */
	private void cleanDumpFile() {
		File log = new File(Environment.getExternalStorageDirectory(),
				"Tracks.txt");
		log.delete();
	}

	/**
	 * Saves a json object to the sd card
	 * 
	 * @param obj
	 *            the object to save
	 */
	private void savetoSdCard(JSONObject obj) {
		File log = new File(Environment.getExternalStorageDirectory(),
				"Tracks.txt");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					log.getAbsolutePath(), true));
			out.write(obj.toString());
			out.newLine();
			out.newLine();
			out.flush();
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Error saving tracks to SD card.", e);
		}
	}

}
