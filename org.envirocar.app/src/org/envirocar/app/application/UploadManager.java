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
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.network.HTTPClient;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackMetadata;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
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
	
	private String url = ECApplication.BASE_URL + "/users/%1$s/tracks";
	private static Logger logger = Logger.getLogger(UploadManager.class);
	private DbAdapter dbAdapter;
	private Context context;
	private static Map<String, String> temporaryAlreadyRegisteredCars = new HashMap<String, String>();


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
		
		new UploadAsyncTask(callback).execute(track);
	}
	
	private void registerCarBeforeUpload(Track track) throws NotConnectedException {
		Car car = track.getCar();
		String tempId = car.getId();
		String sensorIdFromServer = DAOProvider.instance().getSensorDAO().saveSensor(car);

		car.setId(sensorIdFromServer);

		logger.info("Car id tmpTrack: " + track.getCar().getId());

		DbAdapterImpl.instance().updateTrack(track);
		DbAdapterImpl.instance().updateCarIdOfTracks(tempId, car.getId());
		
		/*
		 * we need this hack... Track objects
		 * in memory are not informed through the DB update
		 */
		temporaryAlreadyRegisteredCars.put(tempId, car.getId());
		if (CarManager.instance().getCar().getId().equals(tempId)) {
			CarManager.instance().setCar(car);
		}
	}
	
	private boolean hasTemporaryCar(Track track) {		
		return track.getCar().getId().startsWith(Car.TEMPORARY_SENSOR_ID);
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
			
			if (hasTemporaryCar(track)) {
				/*
				 * perhaps we already did a registration for this temp car.
				 * the Map is application uptime scope (static).
				 */
				if (!temporaryCarAlreadyRegistered(track)) {
					try {
						registerCarBeforeUpload(track);
					} catch (NotConnectedException e) {
						logger.warn(e.getMessage(), e);
						this.cancel(true);
						((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
					}	
				}
				
			}

			File file = null;
			try {
				file = saveTrackAndReturnFile(track);
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
		/*
		 * inject track metadata
		 */
		track.updateMetadata(new TrackMetadata(context));
		
		return new TrackEncoder().createTrackJson(track, isObfuscationEnabled()).toString();
	}
	



	public boolean temporaryCarAlreadyRegistered(Track track) {
		if (temporaryAlreadyRegisteredCars.containsKey(track.getCar().getId())) {
			track.getCar().setId(temporaryAlreadyRegisteredCars.get(track.getCar().getId()));
			return true;
		}
		return false;
	}

	public boolean isObfuscationEnabled() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
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
	private File saveToSdCard(String obj, String id) {
		File log = new File(context.getExternalFilesDir(null),"enviroCar-track-"+id+".json");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), false));
			out.write(obj);
			out.flush();
			out.close();
			return log;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}
	
	public File saveTrackAndReturnFile(Track t) throws JSONException, TrackWithoutMeasurementsException{
		return saveToSdCard(getTrackJSON(t), (t.isRemoteTrack() ? t.getRemoteID() : Long.toString(t.getId())));
	}

}