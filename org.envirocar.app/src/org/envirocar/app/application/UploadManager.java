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

package org.envirocar.app.application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.network.HTTPClient;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
/**
 * Manager that can upload tracks and cars to the server. 
 * Use the uploadAllTracks function to upload all local tracks. 
 * Make sure that you specify the dbAdapter when instantiating.
 * The default constructor should only be used when there is no
 * other way.
 * 
 */
public class UploadManager {

	public static final String NET_ERROR = "net_error";
	public static final String GENERAL_ERROR = "-1";
	private static final Set<PropertyKey> supportedPhenomenons = new HashSet<PropertyKey>();

	private static Logger logger = Logger.getLogger(UploadManager.class);
	
	static {
		supportedPhenomenons.add(PropertyKey.CALCULATED_MAF);
		supportedPhenomenons.add(PropertyKey.MAF);
		supportedPhenomenons.add(PropertyKey.CO2);
		supportedPhenomenons.add(PropertyKey.SPEED);
		supportedPhenomenons.add(PropertyKey.RPM);
		supportedPhenomenons.add(PropertyKey.INTAKE_PRESSURE);
		supportedPhenomenons.add(PropertyKey.INTAKE_TEMPERATURE);
		supportedPhenomenons.add(PropertyKey.CONSUMPTION);
		supportedPhenomenons.add(PropertyKey.ENGINE_LOAD);
		supportedPhenomenons.add(PropertyKey.THROTTLE_POSITON);
		supportedPhenomenons.add(PropertyKey.GPS_ACCURACY);
		supportedPhenomenons.add(PropertyKey.GPS_ALTITUDE);
		supportedPhenomenons.add(PropertyKey.GPS_BEARING);
		supportedPhenomenons.add(PropertyKey.GPS_HDOP);
		supportedPhenomenons.add(PropertyKey.GPS_PDOP);
		supportedPhenomenons.add(PropertyKey.GPS_VDOP);
		supportedPhenomenons.add(PropertyKey.GPS_SPEED);
	}
	
	private String url = ECApplication.BASE_URL + "/users/%1$s/tracks";

	private DbAdapter dbAdapter;
	private Context context;


	/**
	 * Normal constructor for this manager. Specify the context and the dbadapter.
	 * @param dbAdapter The dbadapter (most likely the local one)
	 * @param ctx The context.
	 */
	public UploadManager(Context ctx) {
		this.context = ctx;
		this.dbAdapter = DbAdapterImpl.instance();
	}

	/**
	 * This methods uploads all local tracks to the server
	 */
	public void uploadAllTracks(TrackUploadFinishedHandler callback) {
		for (Track track : dbAdapter.getAllLocalTracks()) {
			uploadSingleTrack(track, callback);
		}
	}
	
	public void uploadSingleTrack(Track track, TrackUploadFinishedHandler callback) {
		if (track == null ) return;
		
		if (isCarOfTrackSavedLocallyOnly(track)) {
			registerCarBeforeUpload(track);
		}
		new UploadAsyncTask(callback).execute(track);
	}
	
	private void registerCarBeforeUpload(Track track) {

		Car car = track.getCar();
		String sensorString = String
				.format(Locale.ENGLISH,
						"{ \"type\": \"%s\", \"properties\": {\"manufacturer\": \"%s\", \"model\": \"%s\", \"fuelType\": \"%s\", \"constructionYear\": %s, \"engineDisplacement\": %s } }",
						CarSelectionPreference.SENSOR_TYPE, car.getManufacturer(), car.getModel(), car.getFuelType(),
						car.getConstructionYear(), car.getEngineDisplacement());
		try {
			String sensorIdFromServer = new SensorUploadTask().execute(
					sensorString).get();

			car.setId(sensorIdFromServer);

			logger.info("Car id tmpTrack: " + track.getCar().getId());

			DbAdapterImpl.instance().updateTrack(track);
			
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		} catch (ExecutionException e) {
			logger.warn(e.getMessage(), e);
		}

	}
	
	private boolean isCarOfTrackSavedLocallyOnly(Track track) {		
		return track.getCar().getId().startsWith(Car.TEMPORARY_SENSOR_ID);
	}
	
	private String registerSensor(String sensorString) throws IOException {
		
		User user = UserManager.instance().getUser();
		String username = user.getUsername();
		String token = user.getToken();
		
		HttpPost postRequest = new HttpPost(
				ECApplication.BASE_URL+"/sensors");
		
		postRequest.addHeader("Content-Type", "application/json");
		
		postRequest.addHeader("Accept-Encoding", "gzip");
		
		if (user != null)
			postRequest.addHeader("X-User", username);
		
		if (token != null)
			postRequest.addHeader("X-Token", token);
		
		StringEntity se = new StringEntity(sensorString);
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		
		postRequest.setEntity(se);
		
		HttpResponse response = HTTPClient.execute(postRequest);
		
		int httpStatusCode = response.getStatusLine().getStatusCode();
		
		Header[] h = response.getAllHeaders();

		String location = "";
		for (int i = 0; i < h.length; i++) {
			if (h[i].getName().equals("Location")) {
				location += h[i].getValue();
				break;
			}
		}
		logger.info(httpStatusCode + " " + location);

		return location.substring(
				location.lastIndexOf("/") + 1,
				location.length());
	}
	
	private class SensorUploadTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			
			try {
				return registerSensor(params[0]);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
			return "";
		}
		
	}
	
	private class UploadAsyncTask extends AsyncTask<Track, Track, Track> {
		
		
		private TrackUploadFinishedHandler callback;

		public UploadAsyncTask(TrackUploadFinishedHandler callback) {
			this.callback = callback;
		}

		@Override
		protected Track doInBackground(Track... params) {
			((ECApplication) context).createNotification("start");
			User user = UserManager.instance().getUser();
			String username = user.getUsername();
			String token = user.getToken();
			String urlL = String.format(url, username);
			
			Track track = params[0];
			Thread.currentThread().setName("TrackUploaderTast-"+track.getId());

			JSONObject trackJSONObject = null;
			try {
				trackJSONObject = createTrackJson(track);
			} catch (JSONException e) {
				logger.warn(e.getMessage(), e);
				//the track wasn't JSON serializable. shouldn't occur.
				this.cancel(true);
				((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
			} catch (RuntimeException e) {
				logger.warn(e.getMessage(), e);
				this.cancel(true);
				((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
			} catch (TrackWithoutMeasurementsException e) {
				if (track.getNumberOfMeasurements() != 0) {
					/*
					 * obfuscation removed all measurements
					 */
					final Activity ac = ((ECApplication) context).getCurrentActivity();
					if (ac != null) {
						ac.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Crouton.makeText(ac, R.string.uploading_track_no_measurements_after_obfuscation_long, Style.ALERT).show();								
							}
						});
					}
					((ECApplication) context).createNotification(context.getResources().getString(R.string.uploading_track_no_measurements_after_obfuscation));
				}
				else {
					logger.warn(e.getMessage(), e);
				}
				this.cancel(true);
			}
			
			//save the track into a json file
			File file = savetoSdCard(trackJSONObject, track.isRemoteTrack() ? track.getRemoteID() : Long.toString(track.getId()));

			if (file == null) {
				this.cancel(true);
				((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
			}
			
			//upload
			String httpResult = sendHttpPost(urlL, file, token, username);
			if (httpResult.equals(NET_ERROR)){
				((ECApplication) context).createNotification(context.getResources().getString(R.string.error_host_not_found));
			} else if (httpResult.equals(GENERAL_ERROR)) {
				((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
			} else {
				/*
				 * success, we got an ID
				 */
				((ECApplication) context).createNotification("success");
				track.setRemoteID(httpResult);
				dbAdapter.updateTrack(track);
				
				if (callback != null) {
					callback.onSuccessfulUpload(track);
				}
			}
			
			return track;
		}
		
	}

	public String getTrackJSON(Track track) throws JSONException, TrackWithoutMeasurementsException {
		return createTrackJson(track).toString();
	}
	
	/**
	 * Converts Track Object into track.create.json string
	 * 
	 * @return
	 * @throws JSONException 
	 * @throws TrackWithoutMeasurementsException 
	 */
	private JSONObject createTrackJson(Track track) throws JSONException, TrackWithoutMeasurementsException {
		JSONObject result = new JSONObject();
		
		String trackSensorName = track.getCar().getId();

		ArrayList<JSONObject> measurementElements = new ArrayList<JSONObject>();
		
		List<Measurement> measurements = getNonObfuscatedMeasurements(track);

		if (measurements == null || measurements.isEmpty()) {
			throw new TrackWithoutMeasurementsException("Track did not contain any non obfuscated measurements.");
		}
			
		
		for (Measurement measurement : measurements) {
			JSONObject measurementJson = createMeasurementJson(track, trackSensorName, measurement);
			measurementElements.add(measurementJson);
		}
		
		result.put("type", "FeatureCollection");
		result.put("features", new JSONArray(measurementElements));
		result.put("properties", createTrackProperties(track, trackSensorName));

		return result;
	}

	private boolean isSpatialObfuscationCandidate(Measurement measurement,
			Track track) {
		return (Util.getDistance(track.getFirstMeasurement(), measurement) <= 0.25)
				|| (Util.getDistance(track.getLastMeasurement(), measurement) <= 0.25);
	}

	private boolean isTemporalObfuscationCandidate(Measurement measurement,
			Track track) throws MeasurementsException {
		return (measurement.getTime() - track.getStartTime() <= 60000 ||
				track.getEndTime() - measurement.getTime() <= 60000);
	}
	
	/**
	 * resolve all not obfuscated measurements of a track.
	 * 
	 * This returns all measurements, if obfuscation is disabled. Otherwise
	 * measurements within the first and last minute and those within the start/end
	 * radius of 250 m are ignored (only if they are in the beginning/end of the track).
	 * 
	 * @param track
	 * @return
	 */
	public List<Measurement> getNonObfuscatedMeasurements(Track track) {
		List<Measurement> measurements = track.getMeasurements();
		
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean obfuscatePositions = preferences.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
		
		if (obfuscatePositions) {
			boolean wasAtLeastOneTimeNotObfuscated = false;
			ArrayList<Measurement> privateCandidates = new ArrayList<Measurement>();
			ArrayList<Measurement> nonPrivateMeasurements = new ArrayList<Measurement>();
			for (Measurement measurement : measurements) {
				try {
					/*
					 * ignore early and late
					 */
					if (isTemporalObfuscationCandidate(measurement, track)) {
						continue;
					}

					/*
					 * ignore distance
					 */
					if (isSpatialObfuscationCandidate(measurement, track)) {
						if (wasAtLeastOneTimeNotObfuscated) {
							privateCandidates.add(measurement);
							nonPrivateMeasurements.add(measurement);
						}
						continue;
					}

					/*
					 * we may have found obfuscation candidates in the middle of the track
					 * (may cross start or end point) in a PRIOR iteration
					 * of this loop. these candidates can be removed now as we are again
					 * out of obfuscation scope
					 */
					if (wasAtLeastOneTimeNotObfuscated) {
						privateCandidates.clear();
					}
					else {
						wasAtLeastOneTimeNotObfuscated = true;
					}
					
					nonPrivateMeasurements.add(measurement);
				} catch (MeasurementsException e) {
					logger.warn(e.getMessage(), e);
				}
				
			}
			/*
			 * the private candidates which have made it until here
			 * shall be ignored
			 */
			nonPrivateMeasurements.removeAll(privateCandidates);
			return nonPrivateMeasurements;
		}
		
		return measurements;
	}

	private JSONObject createTrackProperties(Track track, String trackSensorName) throws JSONException {
		JSONObject result = new JSONObject();
		
		result.put("sensor", trackSensorName);
		result.put("description", track.getDescription());
		result.put("name", track.getName());
		
		return result;
	}

	private JSONObject createMeasurementJson(Track track, String trackSensorName, Measurement measurement) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("type", "Feature");
		
		result.put("geometry", createGeometry(measurement));
		result.put("properties", createMeasurementProperties(measurement, trackSensorName));
		
//		String lat = String.valueOf(measurement.getLatitude());
//		String lon = String.valueOf(measurement.getLongitude());
//		
//		String time = iso8601Format.format(measurement.getTime());
//		
//		StringBuilder phenoms = new StringBuilder();
//		
//		Set<PropertyKey> properties = track.getAllOccurringProperties();
//		for (PropertyKey key : properties) {
//			Double value = measurement.getProperty(key);
//			phenoms.append("\"");
//			phenoms.append(key.toString());
//			phenoms.append("\":{\"value\":");
//			phenoms.append(value != null ? value.toString() : Measurement.NA_VALUE);
//			phenoms.append("},");
////			String propertyJson = String.format("\"%s\":{\"value\":%s},", key.toString(), value != null ? value.toString() : Measurement.NA_VALUE);
////			phenoms.append(propertyJson);
//		}
//		// remove last comma
//		String phenomsJson = phenoms.length() > 0 ? phenoms.substring(0, phenoms.length() - 1) : ""; 
//		String measurementJson = String
//				.format("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[%s,%s]},\"properties\":{\"time\":\"%s\",\"sensor\":\"%s\",\"phenomenons\":{%s}}}",
//						lon, lat, time, trackSensorName, phenomsJson);
		return result;
	}

	private JSONObject createGeometry(Measurement measurement) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("type", "Point");
		
		ArrayList<Double> coords = new ArrayList<Double>(2);
		coords.add(measurement.getLongitude());
		coords.add(measurement.getLatitude());
		
		result.put("coordinates", new JSONArray(coords));
		return result;
	}

	private JSONObject createMeasurementProperties(Measurement measurement, String trackSensorName) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("sensor", trackSensorName);
		JSONObject phens = createPhenomenons(measurement);
		if (phens != null && phens.length() > 0) {
			result.put("phenomenons", phens);
		}
		result.put("time", Util.longToIsoDate(measurement.getTime()));
		return result;
	}

	private JSONObject createPhenomenons(Measurement measurement) throws JSONException {
		JSONObject result = new JSONObject();
		Map<PropertyKey, Double> props = measurement.getAllProperties();
		for (PropertyKey key : props.keySet()) {
			if (supportedPhenomenons.contains(key)) {
				result.put(key.toString(), createValue(props.get(key)));
			}
		}
		return result;
	}

	private JSONObject createValue(Double double1) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("value", double1);
		return result;
	}

	/**
	 * Uploads the json object to the server
	 * 
	 * @param url
	 *            Url
	 * @param contents
	 *            The Json Object
	 * @param xToken
	 *            Token
	 * @param xUser
	 *            Username
	 * @return Server response status code
	 */
	private String sendHttpPost(String url, File contents, String xToken,
			String xUser) {

		try {
			HttpPost httpPostRequest = new HttpPost(url);

			FileEntity se = new FileEntity(contents, "application/json");
			se.setContentType("application/json");

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("X-Token", xToken);
			httpPostRequest.setHeader("X-User", xUser);

			HttpResponse response = (HttpResponse) HTTPClient
					.execute(httpPostRequest);
			
			String location = "";
			Header[] h = response.getAllHeaders();
			for (int i = 0; i< h.length; i++){
				if( h[i].getName().equals("Location")){
					location += h[i].getValue();
					break;
				}
			}
			
			String trackid = location.substring(location.lastIndexOf("/")+1, location.length());


			int statusCode = response.getStatusLine()
					.getStatusCode();
			
			logger.debug("Status Code: "+ statusCode);

			if (statusCode < HttpStatus.SC_MULTIPLE_CHOICES){
				HTTPClient.consumeEntity(response.getEntity());
				return trackid;
			} else {
				String errorResponse = HTTPClient.readResponse(response.getEntity());
				logger.warn("Server response: "+ errorResponse);
				return GENERAL_ERROR;
			}

		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			return GENERAL_ERROR;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			return NET_ERROR;
		}
	}

	/**
	 * Saves a json object to the sd card
	 * 
	 * @param obj
	 *            the object to save
	 * @param id 
	 */
	private File savetoSdCard(JSONObject obj, String id) {
		File log = new File(context.getExternalFilesDir(null),"enviroCar-track-"+id+".json");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), false));
			out.write(obj.toString());
			out.flush();
			out.close();
			return log;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}
	
	public File saveTrackAndReturnFile(Track t) throws JSONException, TrackWithoutMeasurementsException{
		return savetoSdCard(createTrackJson(t), (t.isRemoteTrack() ? t.getRemoteID() : Long.toString(t.getId())));
	}

}