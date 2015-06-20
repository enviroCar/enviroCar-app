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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.envirocar.app.injection.InjectionApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.NotificationHandler;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.DAOProvider.AsyncExecutionWithCallback;
import org.envirocar.app.dao.exception.DAOException;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackMetadata;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Manager that can upload tracks and cars to the server.
 * Use the uploadAllTracks function to upload all local tracks.
 * Make sure that you specify the dbAdapter when instantiating.
 * The default constructor should only be used when there is no
 * other way.
 */
public class UploadManager {

    public static final String NET_ERROR = "net_error";
    public static final String GENERAL_ERROR = "-1";

    private static Logger logger = Logger.getLogger(UploadManager.class);
    private static Map<String, String> temporaryAlreadyRegisteredCars = new HashMap<String, String>();

    @Inject
    protected Activity mActivity;
    @Inject
    @InjectionApplicationScope
    protected Context mContext;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected NotificationHandler mNotificationHandler;
    @Inject
    protected CarManager mCarManager;
    @Inject
    protected DAOProvider mDAOProvider;

    /**
     * Normal constructor for this manager. Specify the context and the dbadapter.
     *
     * @param ctx The context.
     */
    public UploadManager(Context ctx) {
        ((Injector) ctx).injectObjects(this);
    }

    public boolean isObfuscationEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
    }

    /**
     * This methods uploads all local tracks to the server
     */
    public void uploadAllTracks(TrackUploadFinishedHandler callback) {
        for (Track track : mDBAdapter.getAllLocalTracks()) {
            uploadSingleTrack(track, callback);
        }
    }

    public void uploadSingleTrack(final Track track, final TrackUploadFinishedHandler callback) {
        if (track == null) return;

        DAOProvider.async(new AsyncExecutionWithCallback<String>() {

            @Override
            public String execute() throws DAOException {
                Thread.currentThread().setName("TrackUploaderTask-" + track.getTrackId());

                mNotificationHandler.createNotification("start");

				/*
                 * inject track metadata
				 */
                mDBAdapter.updateTrackMetadata(track.getTrackId(), new TrackMetadata(mContext));

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
                    String result = mDAOProvider.getTrackDAO().storeTrack(track,
                            isObfuscationEnabled());
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

                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Crouton.makeText(mActivity,
                                    R.string.uploading_track_no_measurements_after_obfuscation_long,
                                    Style.ALERT).show();
                        }
                    });
                }
                mNotificationHandler.createNotification
                        (mContext.getString(R.string
                                .uploading_track_no_measurements_after_obfuscation));
            }

            @Override
            public String onResult(String result, boolean fail,
                                   Exception e) {
                if (!fail) {
                    /*
                     * success, we got an ID
					 */
                    mNotificationHandler.createNotification("success");
//					track.setRemoteID(result);
//					dbAdapter.updateTrack(track);
                    mDBAdapter.transitLocalToRemoteTrack(track, result);

                    if (callback != null) {
                        callback.onSuccessfulUpload(track);
                    }
                } else {
                    logger.warn(e.getMessage(), e);
                    mNotificationHandler.createNotification(mContext.getString(R.string
                            .general_error_please_report));
                }
                return null;
            }
        });

    }

    private void registerCarBeforeUpload(Track track) throws NotConnectedException, UnauthorizedException {
        Car car = track.getCar();
        String tempId = car.getId();
        String sensorIdFromServer = mDAOProvider.getSensorDAO().saveSensor(car);

        car.setId(sensorIdFromServer);

        logger.info("Car id tmpTrack: " + track.getCar().getId());

        mDBAdapter.updateTrack(track);
        mDBAdapter.updateCarIdOfTracks(tempId, car.getId());
		
		/*
		 * we need this hack... Track objects
		 * in memory are not informed through the DB update
		 */
        temporaryAlreadyRegisteredCars.put(tempId, car.getId());
        if (mCarManager.getCar().getId().equals(tempId)) {
            mCarManager.setCar(car);
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