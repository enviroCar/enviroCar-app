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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.UploadTrackEvent;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.network.HTTPClient;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
/**
 * Manager that can upload a track to the server. 
 * Use the uploadAllTracks function to upload all local tracks. 
 * Make sure that you specify the dbAdapter when instantiating.
 * The default constructor should only be used when there is no
 * other way.
 * 
 */
public class UploadManager {

	private static Logger logger = Logger.getLogger(UploadManager.class);
	

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
	public void uploadAllTracks() {
		for (Track track : dbAdapter.getAllLocalTracks()) {
			new UploadAsyncTask().execute(track);
		}
	}
	
	public void uploadSingleTrack(Track track){
		new UploadAsyncTask().execute(track);
	}

	private class UploadAsyncTask extends AsyncTask<Track, Void, Track> {
		
		@Override
		protected Track doInBackground(Track... params) {
			
			//probably unnecessary
			if(dbAdapter.getNumberOfRemoteTracks() == 0)
				this.cancel(true);
			
			User user = UserManager.instance().getUser();
			String username = user.getUsername();
			String token = user.getToken();
			String urlL = String.format(url, username);
			
			Track track = params[0];

			JSONObject trackJSONObject = null;
			try {
				trackJSONObject = createTrackJson(track);
			} catch (JSONException e) {
				logger.warn(e.getMessage(), e);
				//the track wasn't JSON serializable. shouldn't occur.
				this.cancel(true);
				((ECApplication) context).createNotification("General Track error (JSON) Please contact envirocar.org");
			}
			// don upload track if it has no measurements
			if(track.getNumberOfMeasurements() != 0) {
				//save the track into a json file
				savetoSdCard(trackJSONObject,track.getId());
				//upload
				String httpResult = sendHttpPost(urlL, trackJSONObject, token, username);
				if (httpResult.equals("net_error")){
					((ECApplication) context).createNotification(context.getResources().getString(R.string.error_host_not_found));
				} else if (!httpResult.equals("-1")) {
					((ECApplication) context).createNotification("success");
					track.setRemoteID(httpResult);
					dbAdapter.updateTrack(track);
					EventBus.getInstance().fireEvent(new UploadTrackEvent(track));
				} else {
					((ECApplication) context).createNotification("General Track error. Please contact envirocar.org");
				}
			}
			return null;
		}
	}

	public String getTrackJSON(Track track) throws JSONException{
		return createTrackJson(track).toString();
	}
	
	/**
	 * Converts Track Object into track.create.json string
	 * 
	 * @return
	 * @throws JSONException 
	 */
	private JSONObject createTrackJson(Track track) throws JSONException {

		String trackName = track.getName();
		String trackDescription = track.getDescription();
		String trackSensorName = track.getCar().getId();

		String trackElementJson = String
				.format("{\"type\":\"FeatureCollection\",\"properties\":{\"name\":\"%s\",\"description\":\"%s\",\"sensor\":\"%s\"},\"features\":[",
						trackName, trackDescription, trackSensorName);

		ArrayList<Measurement> measurements = track.getMeasurements();
		ArrayList<String> measurementElements = new ArrayList<String>();
		
		// Cut-off first and last minute of tracks that are longer than 3
		// minutes. Also cut of these measurements if they are closer than 250m
		// to the start and the end.
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		boolean obfuscatePositions = preferences.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
		
		try {
			if (track.getEndTime() - track.getStartTime() > 180000) {
				ArrayList<Measurement> privateMeasurements = new ArrayList<Measurement>();
				for (Measurement measurement : measurements) {
					try {
						if (obfuscatePositions) {
							if (measurement.getTime() - track.getStartTime() > 60000 && track.getEndTime() - measurement.getTime() > 60000) {
								if ((Utils.getDistance(track.getFirstMeasurement().getLatitude(), track.getFirstMeasurement().getLongitude(), measurement.getLatitude(), measurement.getLongitude()) > 0.25) && (Utils.getDistance(track.getLastMeasurement().getLatitude(), track.getLastMeasurement().getLongitude(), measurement.getLatitude(), measurement.getLongitude()) > 0.25)) {
									privateMeasurements.add(measurement);
								}
							}
						} else {
							privateMeasurements.add(measurement);
						}

					} catch (MeasurementsException e) {
						logger.warn(e.getMessage(), e);
					}
				}
				measurements = privateMeasurements;
			}
		} catch (MeasurementsException e) {
			logger.warn(e.getMessage(), e);
		}

		for (Measurement measurement : measurements) {
			String measurementJson = createMeasurementJson(track, trackSensorName, measurement);
			measurementElements.add(measurementJson);
		}

		String measurementElementsJson = TextUtils.join(",", measurementElements);
		logger.debug("measurementElem "+measurementElementsJson);
		String closingElementJson = "]}";

		String trackString = String.format("%s %s %s", trackElementJson,
				measurementElementsJson, closingElementJson);
		logger.debug("Track "+trackString);

		return new JSONObject(trackString);
	}

	private String createMeasurementJson(Track track, String trackSensorName, Measurement measurement) {
		String lat = String.valueOf(measurement.getLatitude());
		String lon = String.valueOf(measurement.getLongitude());
		DateFormat dateFormat1 = new SimpleDateFormat("y-MM-d", Locale.ENGLISH);
		DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		dateFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = dateFormat1.format(measurement.getTime()) + "T" + dateFormat2.format(measurement.getTime()) + "Z";
		StringBuilder phenoms = new StringBuilder();
		Map<PropertyKey, Double> properties = measurement.getAllProperties();
		for (PropertyKey key : properties.keySet()) {
			String propertyJson = String.format("\"%s\":{\"value\":%s},", key.toString(), properties.get(key));
			phenoms.append(propertyJson);
		}
		// remove last comma
		String phenomsJson = phenoms.length() > 0 ? phenoms.substring(0, phenoms.length() - 1) : ""; 
		String measurementJson = String
				.format("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[%s,%s]},\"properties\":{\"time\":\"%s\",\"sensor\":\"%s\",\"phenomenons\":{%s}}}",
						lon, lat, time, trackSensorName, phenomsJson);
		return measurementJson;
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
	private String sendHttpPost(String url, JSONObject jsonObjSend, String xToken,
			String xUser) {

		try {
			HttpPost httpPostRequest = new HttpPost(url);

			StringEntity se = new StringEntity(jsonObjSend.toString());
			se.setContentType("application/json");
			logger.debug("SE"+ jsonObjSend.toString());

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


			String statusCode = String.valueOf(response.getStatusLine()
					.getStatusCode());
			
			HTTPClient.consumeEntity(response.getEntity());

			logger.debug(statusCode);

			if(statusCode.equals("201")){
				return trackid;
			} else {
				return "-1";
			}

		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			return "-1";
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			return "net_error";
		}
	}

	/**
	 * Saves a json object to the sd card
	 * 
	 * @param obj
	 *            the object to save
	 */
	private File savetoSdCard(JSONObject obj, long fileid) {
		File log = new File(context.getExternalFilesDir(null),"envirocar_track"+fileid+".json");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), true));
			out.write(obj.toString());
			out.flush();
			out.close();
			return log;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}
	
	public File saveTrackAndReturnUri(Track t) throws JSONException{
		return savetoSdCard(createTrackJson(t), t.getId());
	}

}