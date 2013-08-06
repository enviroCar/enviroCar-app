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
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.envirocar.app.R;
import org.envirocar.app.activity.ListMeasurementsFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapterLocal;
import org.envirocar.app.storage.DbAdapterRemote;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
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
	
	private static final String TAG = "uploadmanager";

	private String url = ECApplication.BASE_URL + "/users/%1$s/tracks";

	private DbAdapterLocal dbAdapterLocal;
	private DbAdapterRemote dbAdapterRemote;
	private Context context;
	
	/**
	 * Normal constructor for this manager. Specify the context and the dbadapter.
	 * @param dbAdapter The dbadapter (most likely the local one)
	 * @param ctx The context.
	 */
	public UploadManager(Context ctx) {
		this.context = ctx;
		this.dbAdapterLocal = (DbAdapterLocal) ((ECApplication) context).getDbAdapterLocal();
		this.dbAdapterRemote = (DbAdapterRemote) ((ECApplication) context).getDbAdapterRemote();
	}

	/**
	 * This methods uploads all local tracks to the server
	 */
	public void uploadAllTracks() {
		new UploadAsyncTask().execute(dbAdapterLocal.getAllTracks());
	}
	
	public void uploadSingleTrack(Track track){
		ArrayList<Track> t = new ArrayList<Track>(1);
		t.add(track);
		new UploadAsyncTask().execute(t);
	}
	

	private class UploadAsyncTask extends AsyncTask<ArrayList<Track>, Void, Void> {
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			((ListMeasurementsFragment) ((FragmentActivity) ((ECApplication) context).getCurrentActivity()).getSupportFragmentManager().findFragmentByTag("MY_TRACKS")).notifyDataSetChanged();
		}

		@Override
		protected Void doInBackground(ArrayList<Track>... params) {
			
			//probably unnecessary
			if(dbAdapterLocal.getNumberOfStoredTracks() == 0)
				this.cancel(true);
			
			String username = ((ECApplication) context).getUser().getUsername();
			String token = ((ECApplication) context).getUser().getToken();
			String urlL = String.format(url, username);


			//iterate through the list of tracks :)
			for(Track t : params[0]){
				JSONObject trackJSONObject = null;
				try {
					trackJSONObject = createTrackJson(t);
				} catch (JSONException e) {
					logger.warn(e.getMessage(), e);
					//the track wasn't JSON serializable. shouldn't occur.
					this.cancel(true);
					((ECApplication) context).createNotification("General Track error (JSON) Please contact envirocar.org");
				}
				//try next track if the track has no measurements
				if(t.getNumberOfMeasurements() == 0)
					continue;
				
				//save the track into a json file
				savetoSdCard(trackJSONObject,t.getId());
				//upload
				String httpResult = sendHttpPost(urlL, trackJSONObject, token, username);
				if (httpResult.equals("net_error")){
					((ECApplication) context).createNotification(context.getResources().getString(R.string.error_host_not_found));
				} else if (!httpResult.equals("-1")) {
					((ECApplication) context).createNotification("success");
					dbAdapterLocal.deleteTrack(t.getId());
					t.setId(httpResult);
					dbAdapterRemote.insertTrackWithMeasurements(t);
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
		String trackSensorName = track.getSensorID();

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
							if (measurement.getMeasurementTime() - track.getStartTime() > 60000 && track.getEndTime() - measurement.getMeasurementTime() > 60000) {
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

		for (int i = 0; i < measurements.size(); i++) {
			String lat = String.valueOf(measurements.get(i).getLatitude());
			String lon = String.valueOf(measurements.get(i).getLongitude());
			DateFormat dateFormat1 = new SimpleDateFormat("y-MM-d");
			DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");
			dateFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time = dateFormat1.format(measurements.get(i)
					.getMeasurementTime())
					+ "T"
					+ dateFormat2.format(measurements.get(i)
							.getMeasurementTime()) + "Z";
			
			String speed = String.valueOf(measurements.get(i).getSpeed());
			String rpm = String.valueOf(measurements.get(i).getRpm());
			String intake_temperature = String.valueOf(measurements.get(i).getIntakeTemperature());
			String intake_pressure = String.valueOf(measurements.get(i).getIntakePressure());
			//no maf? no json! :)
			if(measurements.get(i).getMaf() > 0){
				String co2 = "0", consumption = "0";
				try {
					//TODO
					//Why get this here when it is already set in ECApplication.java?
					//Also if this wasn't set in ECApplication we should get FIRST consumption then co2 because of the way the methods are defined in Track.java
					
					co2 = String.valueOf(track.getCO2EmissionOfMeasurement(i));
					consumption = String.valueOf(track
							.getFuelConsumptionOfMeasurement(i));
				} catch (FuelConsumptionException e) {
					logger.warn(e.getMessage(), e);
				}

				String maf = String.valueOf(measurements.get(i).getMaf());
				String measurementJson = String
						.format("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[%s,%s]},\"properties\":{\"time\":\"%s\",\"sensor\":\"%s\",\"phenomenons\":{\"CO2\":{\"value\":%s},\"Consumption\":{\"value\":%s},\"MAF\":{\"value\":%s},\"Speed\":{\"value\":%s}, \"Rpm\": { \"value\": %s}, \"Intake Temperature\": { \"value\": %s}, \"Intake Pressure\": { \"value\": %s}}}}",
								lon, lat, time, trackSensorName, co2, consumption,
								maf, speed, rpm, intake_temperature, intake_pressure);
				measurementElements.add(measurementJson);
			} else {
				String co2 = "0", consumption = "0";
				try {
					consumption = String.valueOf(track
							.getFuelConsumptionOfMeasurement(i));
					co2 = String.valueOf(track.getCO2EmissionOfMeasurement(i));
				} catch (FuelConsumptionException e) {
					logger.warn(e.getMessage(), e);
				}

				String calculatedMaf = String.valueOf(measurements.get(i).getCalculatedMaf());
				String measurementJson = String
						.format("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[%s,%s]},\"properties\":{\"time\":\"%s\",\"sensor\":\"%s\",\"phenomenons\":{\"CO2\":{\"value\":%s},\"Consumption\":{\"value\":%s},\"Calculated MAF\":{\"value\":%s},\"Speed\":{\"value\":%s}, \"Rpm\": { \"value\": %s}, \"Intake Temperature\": { \"value\": %s}, \"Intake Pressure\": { \"value\": %s}}}}",
								lon, lat, time, trackSensorName, co2, consumption,
								calculatedMaf, speed, rpm, intake_temperature, intake_pressure);
				measurementElements.add(measurementJson);
			}
			

			

			
		}

		String measurementElementsJson = TextUtils.join(",",
				measurementElements);
		logger.debug("measurementElem "+measurementElementsJson);
		String closingElementJson = "]}";

		String trackString = String.format("%s %s %s", trackElementJson,
				measurementElementsJson, closingElementJson);
		logger.debug("Track "+trackString);

		return new JSONObject(trackString);
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
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPostRequest = new HttpPost(url);

			StringEntity se = new StringEntity(jsonObjSend.toString());
			se.setContentType("application/json");
			logger.debug("SE"+ jsonObjSend.toString());

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("X-Token", xToken);
			httpPostRequest.setHeader("X-User", xUser);

			HttpResponse response = (HttpResponse) httpclient
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

			logger.debug(String.format("%s", statusCode));

			if(statusCode.equals("201")){
				return trackid;
			} else {
				return "-1";
			}

		} catch (UnknownHostException e) {
			logger.warn(e.getMessage(), e);
			return "net_error";
		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			return "-1";
		} catch (ClientProtocolException e) {
			logger.warn(e.getMessage(), e);
			return "net_error";
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
	private File savetoSdCard(JSONObject obj, String fileid) {
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