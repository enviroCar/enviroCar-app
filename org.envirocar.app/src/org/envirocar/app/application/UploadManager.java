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

import java.util.HashMap;
import java.util.Map;

import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.DAOProvider.AsyncExecutionWithCallback;
import org.envirocar.app.dao.exception.DAOException;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackMetadata;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

	public boolean isObfuscationEnabled() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
	}
	
	/**
	 * This methods uploads all local tracks to the server
	 */
	public void uploadAllTracks(TrackUploadFinishedHandler callback) {
		for (Track track : dbAdapter.getAllLocalTracks()) {
			uploadSingleTrack(track, callback);
		}
	}
	
	public void uploadSingleTrack(final Track track, final TrackUploadFinishedHandler callback) {
		if (track == null ) return;
		
		DAOProvider.async(new AsyncExecutionWithCallback<String>() {

			@Override
			public String execute() throws DAOException {
				Thread.currentThread().setName("TrackUploaderTask-"+track.getId());
				
				/*
				 * inject track metadata
				 */
				track.updateMetadata(new TrackMetadata(context));
				
				if (hasTemporaryCar(track)) {
					/*
					 * perhaps we already did a registration for this temp car.
					 * the Map is application uptime scope (static).
					 */
					if (!temporaryCarAlreadyRegistered(track)) {
						registerCarBeforeUpload(track);
					}
					
				}
				
				try {
					String result = DAOProvider.instance().getTrackDAO().storeTrack(track, isObfuscationEnabled());
					return result;
				} catch (TrackWithoutMeasurementsException e) {
					if (track.getNumberOfMeasurements() != 0) {
						alertOnObfuscationMeasurements();
					}
					throw new DAOException(e);
				}
			}

			private void alertOnObfuscationMeasurements() {
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

			@Override
			public String onResult(String result, boolean fail,
					Exception e) {
				if (!fail) {
					/*
					 * success, we got an ID
					 */
					((ECApplication) context).createNotification("success");
					track.setRemoteID(result);
					dbAdapter.updateTrack(track);
					
					if (callback != null) {
						callback.onSuccessfulUpload(track);
					}
				}
				else {
					logger.warn(e.getMessage(), e);
					((ECApplication) context).createNotification(context.getResources().getString(R.string.general_error_please_report));
				}
				return null;
			}
		});
		
	}
	
	private void registerCarBeforeUpload(Track track) throws NotConnectedException, UnauthorizedException {
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
	

	public boolean temporaryCarAlreadyRegistered(Track track) {
		if (temporaryAlreadyRegisteredCars.containsKey(track.getCar().getId())) {
			track.getCar().setId(temporaryAlreadyRegisteredCars.get(track.getCar().getId()));
			return true;
		}
		return false;
	}

}