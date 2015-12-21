/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.services.NotificationHandler;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;
import org.envirocar.core.utils.TrackUtils;
import org.envirocar.remote.DAOProvider;
import org.envirocar.storage.EnviroCarDB;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

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


    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected EnviroCarDB mEnviroCarDB;
    @Inject
    protected NotificationHandler mNotificationHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected TrackHandler mTrackHandler;

    private final Scheduler.Worker mainthreadWorker = AndroidSchedulers.mainThread().createWorker();

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

    private Observable<TrackMetadata> updateTrackMetadataObservable(Track track) {
        return Observable.just(track)
                .map(track1 -> new TrackMetadata(Util.getVersionString(mContext),
                        mUserManager.getUser().getTermsOfUseVersion()))
                .flatMap(trackMetadata -> mTrackHandler.updateTrackMetadata(track.getTrackID(),
                        trackMetadata));
    }

    private void updateTrackMetadata(Track track) {
        TrackMetadata metadata = new TrackMetadata(Util.getVersionString(mContext),
                mUserManager.getUser().getTermsOfUseVersion());
        metadata = mTrackHandler
                .updateTrackMetadata(track.getTrackID(), metadata)
                .toBlocking()
                .first();
        track.setMetadata(metadata);
    }


    public Observable<Track> uploadTracks(final List<Track> tracks) {
        Preconditions.checkNotNull(tracks, "Input tracks cannot be null");

        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                mNotificationHandler.createNotification("start");

                for (Track track : tracks) {
                    // First, update the metadata of the track.
                    updateTrackMetadata(track);

                    try {
                        // assert the car of the track for validity.
                        mCarManager.assertTemporaryCar(track.getCar())
                                .toBlocking()
                                .first();

                        String result = null;
                        if (isObfuscationEnabled()) {
                            logger.info("Obfuscation enabled! Calling TrackUtils" +
                                    ".getObfuscatedTrack()");
                            Track obfuscatedTrack = TrackUtils.getObfuscatedTrack(track);
                            if (obfuscatedTrack.getMeasurements().size() == 0) {
                                throw new NoMeasurementsException("Track has no measurements " +
                                        "after obfuscation.");
                            }
                            result = mDAOProvider.getTrackDAO().createTrack(obfuscatedTrack);
                        } else {
                            logger.info("Obfuscation not enabled!");
                            result = mDAOProvider.getTrackDAO().createTrack(track);
                        }

                        // When successfully updated, then transit the track from local to remote.
                        if(result != null) {
                            track.setRemoteID(result);
                            mEnviroCarDB.updateTrack(track);
                        }

                        // Inform the subscriber about the successful transition.
                        subscriber.onNext(track);
                    } catch (ResourceConflictException e) {
                        logger.error(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    } catch (NotConnectedException e) {
                        logger.error(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    } catch (DataCreationFailureException e) {
                        logger.error(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    } catch (UnauthorizedException e) {
                        logger.error(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    } catch (NoMeasurementsException e) {
                        logger.error(e.getMessage(), e);
                        subscriber.onError(e);
                        continue;
                    }
                }

                subscriber.onCompleted();
            }
        });
    }

    public Observable<Track> uploadSingleTrack(final Track track) {
        Preconditions.checkNotNull(track, "Track to upload cannot be null.");
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.onStart();

                subscriber.add(updateTrackMetadataObservable(track)
                        .flatMap(trackMetadata -> mCarManager.assertTemporaryCar(track.getCar()))
                                // Set the car reference
                        .map(car -> {
                            track.setCar(car);
                            return track;
                        })
                                // obfuscate the track.
                        .map(asObfuscatedTrackWhenChecked())
                                // Upload the track
                        .flatMap(track1 -> mDAOProvider.getTrackDAO().createTrackObservable(track1))
                                // Update the database entry
                        .flatMap(track1 -> mEnviroCarDB.updateTrackObservable(track1))
                                // Subscribe
                        .subscribe(new Subscriber<Track>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                logger.error(e.getMessage(), e);
                                if (e instanceof NoMeasurementsException) {
                                    mainthreadWorker.schedule(() -> Toast.makeText(mContext,
                                            R.string.uploading_track_no_measurements_after_obfuscation_long,
                                            Toast.LENGTH_LONG).show());
                                    mNotificationHandler.createNotification
                                            (mContext.getString(R.string
                                                    .uploading_track_no_measurements_after_obfuscation));
                                } else {
                                    subscriber.onError(e);
                                }
                            }

                            @Override
                            public void onNext(Track track) {
                                logger.info("track has been successful uploaded");
                                subscriber.onNext(track);
                                subscriber.unsubscribe();
                            }
                        }));
            }
        });
    }

    private Func1<Track, Track> asObfuscatedTrackWhenChecked() {
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track track) {
                logger.info("asObfuscatedTrackWhenChecked()");
                if (isObfuscationEnabled()) {
                    logger.info("obfuscation is enabled.");
                    try {
                        return TrackUtils.getObfuscatedTrack(track);
                    } catch (NoMeasurementsException e) {
                        OnErrorThrowable.from(e);
                        return track;
                    }
                } else {
                    logger.info("obfuscation is disabled.");
                    return track;
                }
            }
        };
    }


//    public void uploadSingleTrack(final Track track, final TrackHandler.TrackUploadCallback
//            callback) {
//        if (track == null) return;
//
//        new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                Thread.currentThread().setName("TrackUploaderTask-" + track.getTrackID());
//                callback.onUploadStarted(track);
////                mNotificationHandler.createNotification("start");
//
//                // inject track metadata
//                updateTrackMetadata(track);
//
//                try {
//                    if (hasTemporaryCar(track)) {
//                    /*
//                     * perhaps we already did a registration for this temp car.
//					 * the Map is application uptime scope (static).
//					 */
//                        if (!temporaryCarAlreadyRegistered(track)) {
//                            registerCarBeforeUpload(track);
//                        }
//                    }
//
//                    String result = null;
//                    if (isObfuscationEnabled()) {
//                        Track obfuscatedTrack = TrackUtils.getObfuscatedTrack(track);
//                        result = mDAOProvider.getTrackDAO().createTrack(obfuscatedTrack);
//                    } else {
//                        result = mDAOProvider.getTrackDAO().createTrack(track);
//                    }
//
////                    mNotificationHandler.createNotification("success");
//                    //					track.setRemoteID(result);
//                    //					dbAdapter.updateTrack(track);
//                    mDBAdapter.transitLocalToRemoteTrack(track, result);
//
//                    if (callback != null) {
//                        callback.onSuccessfulUpload(track);
//                    }
//                } catch (Exception e) {
//                    if (track.getMeasurements().size() != 0) {
//                        alertOnObfuscationMeasurements();
//                    }
//                    logger.error(e.getMessage(), e);
//                    mNotificationHandler.createNotification(mContext
//                            .getString(R.string
//                                    .general_error_please_report));
//                }
//
//                return null;
//            }
//
//            private void alertOnObfuscationMeasurements() {
//                /*
//                 * obfuscation removed all measurements
//				 */
//
//                mainthreadWorker.schedule(new Action0() {
//                    @Override
//                    public void call() {
//                        Toast.makeText(mContext,
//                                R.string.uploading_track_no_measurements_after_obfuscation_long,
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//                mNotificationHandler.createNotification
//                        (mContext.getString(R.string
//                                .uploading_track_no_measurements_after_obfuscation));
//            }
//        }.execute();
//    }
}
