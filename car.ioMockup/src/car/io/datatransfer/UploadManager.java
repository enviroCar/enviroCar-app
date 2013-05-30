package car.io.datatransfer;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import car.io.activity.MainActivity;
import car.io.adapter.DbAdapter;
import car.io.adapter.DbAdapterLocal;
import car.io.adapter.Measurement;
import car.io.adapter.Track;
import car.io.application.ECApplication;
import car.io.exception.LocationInvalidException;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class UploadManager {
	
	private static final String TAG = "UploadManager";
	
	// TODO Configure Url in property document/shared preferences
	private String url = "http://giv-car.uni-muenster.de:8080/stable/rest/users/uploaduser/tracks";
	
	private ECApplication application;
	private DbAdapter dbAdapter;
	
	public void uploadAllTracks() {
		
		//TODO fetch all Tracks from LocalDbAdapter
		//TODO DbAdapterLocal as Singleton DbAdapterLocal.getAllTracks
		dbAdapter = MainActivity.application.getInstance().getDbAdapterLocal();
		
		Track dummyTrack = new Track("VIN", "Diesel", dbAdapter);
		dummyTrack.setDescription("This is a description of the track.");
		dummyTrack.setName("This is the Name of the track");
		
		try {
			Measurement dummyMeasurement = new Measurement(12.365f, 24.068f);
			dummyMeasurement.setSpeed(140);
			dummyTrack.addMeasurement(dummyMeasurement);
			
			Measurement dummyMeasurement2 = new Measurement(55.365f, 7.068f);
			dummyMeasurement2.setSpeed(150);
			dummyTrack.addMeasurement(dummyMeasurement2);
			
			Log.i(TAG, "Measurement Objekt erstellt.");
		} catch (LocationInvalidException e1) {
			Log.e(TAG, "Measurement Objekterstellung failed.");
			e1.printStackTrace();
		}
		
		ArrayList<Track> trackList = new ArrayList<Track>();
//		ArrayList<Track> trackList = MainActivity.application.getInstance().getDbAdapterLocal().getAllTracks();
		trackList.add(dummyTrack);
		
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
			
			// TODO Configure X-User, X-Token in property document/shared preferences
			HttpClient.SendHttpPost(url, obj, "tokenxyza", "uploaduser");
		}
	}
	
	
	/**
	 * Converts Track Object into track.create.json string
	 * 
	 * TODO Outsource JSON converting to Converter class 
	 * 
	 * @return 
	 */
	private String createTrackJson(Track track){
		
		String trackName = track.getName();
		String trackDescription = track.getDescription();
		
		//TODO configure sensorName in Track Class.
		//TODO Error Handling: only registered sensor names are accepted from server side
		String trackSensorName = "testsensor1";
		
		String trackElementJson = String.format("{ \"type\": \"FeatureCollection\", \"properties\": { \"name\": \"%s\", \"description\": \"%s\", \"sensor\": \"%s\" }, \"features\": [", trackName, trackDescription, trackSensorName);
		
		ArrayList<Measurement> measurements = track.getMeasurements();
		ArrayList<String> measurementElements = new ArrayList<String>();
		
		for (Measurement measurement : measurements) {
			String lat = String.valueOf(measurement.getLatitude());
			String lon = String.valueOf(measurement.getLongitude());
			// TODO Format change a la 2013-05-16T02:13:27Z needed
			String time = String.valueOf(measurement.getMeasurementTime());
			String sensorNameMeasurement = "testsensor1";
			String speed = String.valueOf(measurement.getSpeed());
			String measurementJson = String.format("{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %s, %s ] }, \"properties\": { \"time\": \"2013-05-16T02:13:27Z\", \"sensor\": { \"name\": \"testsensor1\", \"unit\": \"some\" }, \"phenomenons\": { \"testphenomenon1\": { \"value\": %s } } } }", lat, lon, speed);
			measurementElements.add(measurementJson);
		}
		
		String measurementElementsJson = TextUtils.join(",", measurementElements);
		Log.d("measurementElem", measurementElementsJson);	
		String closingElementJson = "]}";
		
		String trackString = String.format("%s %s %s", trackElementJson, measurementElementsJson, closingElementJson);
		Log.d("Track", trackString);
		
		return trackString;
	}
	
}
