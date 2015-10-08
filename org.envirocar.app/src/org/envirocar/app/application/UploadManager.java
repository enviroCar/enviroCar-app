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
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.TrackUtils;
import org.envirocar.core.util.Util;
import org.envirocar.app.injection.DAOProvider;

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
    private static Map<String, String> temporaryAlreadyRegisteredCars = new HashMap<String,
            String>();

    @Inject
    protected Activity mActivity;
    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected NotificationHandler mNotificationHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected UserManager mUserManager;

    /**
     * Normal constructor for this manager. Specify the context and the dbadapter.
     *
     * @param ctx the context of the current scope
     */
    public UploadManager(Context ctx) {
        ((Injector) ctx).injectObjects(this);
    }

    public boolean isObfuscationEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean(PreferenceConstants.OBFUSCATE_POSITION, false);
    }

    /**
     * This methods uploads all local tracks to the server
     */
    public void uploadAllTracks(TrackHandler.TrackUploadCallback callback) {
        for (Track track : mDBAdapter.getAllLocalTracks()) {
            uploadSingleTrack(track, callback);
        }
    }

    public void uploadSingleTrack(final Track track, final TrackHandler.TrackUploadCallback
            callback) {
        if (track == null) return;

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Thread.currentThread().setName("TrackUploaderTask-" + track.getTrackID());
                callback.onUploadStarted(track);
                mNotificationHandler.createNotification("start");


				/*
                 * inject track metadata
				 */

                mDBAdapter.updateTrackMetadata(track.getTrackID(),
                        new TrackMetadata(Util.getVersionString(mContext),
                                mUserManager.getUser().getTermsOfUseVersion()));

                try {
                    if (hasTemporaryCar(track)) {
                    /*
                     * perhaps we already did a registration for this temp car.
					 * the Map is application uptime scope (static).
					 */
                        if (!temporaryCarAlreadyRegistered(track)) {
                            registerCarBeforeUpload(track);
                        }
                    }

                    String result = null;
                    if (isObfuscationEnabled()) {
                        Track obfuscatedTrack = TrackUtils.getObfuscatedTrack(track);
                        result = mDAOProvider.getTrackDAO().createTrack(obfuscatedTrack);
                    } else {
                        result = mDAOProvider.getTrackDAO().createTrack(track);
                    }

                    mNotificationHandler.createNotification("success");
                    //					track.setRemoteID(result);
                    //					dbAdapter.updateTrack(track);
                    mDBAdapter.transitLocalToRemoteTrack(track, result);

                    if (callback != null) {
                        callback.onSuccessfulUpload(track);
                    }
                } catch (Exception e) {
                    if (track.getMeasurements().size() != 0) {
                        alertOnObfuscationMeasurements();
                    }
                    logger.error(e.getMessage(), e);
                    mNotificationHandler.createNotification(mContext
                            .getString(R.string
                                    .general_error_please_report));
                }

                return null;
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
        }.execute();
    }

    private void registerCarBeforeUpload(Track track) throws NotConnectedException,
            UnauthorizedException, DataCreationFailureException {
        Car car = track.getCar();
        String tempId = car.getId();
        String sensorIdFromServer = mDAOProvider.getSensorDAO().createCar(car);

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
            //        if (true) {
            mCarManager.setCar(car);
        }
    }

    private boolean hasTemporaryCar(Track track) {
        //        return true;
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