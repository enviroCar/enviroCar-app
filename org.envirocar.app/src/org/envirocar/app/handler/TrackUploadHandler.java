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

import android.app.Activity;
import android.content.Context;

import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.app.services.NotificationHandler;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.TrackWithNoValidCarException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.core.utils.TrackUtils;
import org.envirocar.remote.DAOProvider;
import org.envirocar.storage.EnviroCarDB;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

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
@Singleton
public class TrackUploadHandler {
    private static Logger LOG = Logger.getLogger(TrackUploadHandler.class);

    private final Context mContext;
    private final EnviroCarDB mEnviroCarDB;
    private final NotificationHandler mNotificationHandler;
    private final CarPreferenceHandler mCarManager;
    private final DAOProvider mDAOProvider;
    private final TrackDAOHandler trackDAOHandler;
    private final UserHandler mUserManager;
    private final TrackRecordingHandler mTrackRecordingHandler;
    private final TermsOfUseManager mTermsOfUseManager;

    private final Scheduler.Worker mainthreadWorker = AndroidSchedulers.mainThread().createWorker();

    /**
     * Normal constructor for this manager. Specify the context and the dbadapter.
     *
     * @param context the context of the current scope
     */
    @Inject
    public TrackUploadHandler(
            @InjectApplicationScope Context context,
            EnviroCarDB enviroCarDB,
            NotificationHandler notificationHandler,
            CarPreferenceHandler carPreferenceHandler,
            DAOProvider daoProvider,
            TrackDAOHandler trackDAOHandler,
            UserHandler userHandler,
            TrackRecordingHandler trackRecordingHandler,
            TermsOfUseManager termsOfUseManager) {
        this.mContext = context;
        this.mEnviroCarDB = enviroCarDB;
        this.mNotificationHandler = notificationHandler;
        this.mCarManager = carPreferenceHandler;
        this.mDAOProvider = daoProvider;
        this.trackDAOHandler = trackDAOHandler;
        this.mUserManager = userHandler;
        this.mTrackRecordingHandler = trackRecordingHandler;
        this.mTermsOfUseManager = termsOfUseManager;
    }


    public Observable<Track> uploadSingleTrack(Track track, Activity activity) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                LOG.info("uploadSingleTrack() start uploading.");
                subscriber.onStart();

                // Create a dialog with which the user can accept the terms of use.
                subscriber.add(Observable.just(track)
                        // general validation of the track
                        .map(validateRequirementsForUpload())
                        // Verify wether the TermsOfUSe have been accepted.
                        // When the TermsOfUse have not been accepted, create an
                        // Dialog to accept and continue when the user has accepted.
                        .flatMap(track1 ->
                                mTermsOfUseManager.verifyTermsOfUse(activity, track1))
                        // Continue when the TermsOfUse has been accepted, otherwise
                        // throw an error
                        .flatMap(track1 -> track1 != null ? uploadTrack(track) :
                                Observable.error(new NotAcceptedTermsOfUseException(
                                        "Not accepted TermsOfUse")))
                        .subscribe(new Subscriber<Track>() {
                                       @Override
                                       public void onCompleted() {
                                           subscriber.onCompleted();
                                       }

                                       @Override
                                       public void onError(Throwable e) {
                                           subscriber.onError(e);
                                           subscriber.unsubscribe();
                                       }

                                       @Override
                                       public void onNext(Track track) {
                                           subscriber.onNext(track);
                                           subscriber.onCompleted();
                                       }
                                   }
                        ));
            }
        });
    }

    public Observable<Track> uploadAllTracks() {
        return mEnviroCarDB.getAllLocalTracks()
                .flatMap(tracks -> uploadMultipleTracks(tracks));
    }

    public Observable<Track> uploadMultipleTracks(List<Track> tracks) {
        Preconditions.checkState(tracks != null && !tracks.isEmpty(),
                "Input tracks cannot be null or empty.");
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.onStart();
//                mNotificationHandler.createNotification("start");

//                subscriber.add(Observable.from(tracks)
//                        .concatMap(track -> uploadTrack(track))
//                        .subscribe(new Subscriber<Track>() {
//                            @Override
//                            public void onCompleted() {
//                                subscriber.onCompleted();
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                                LOG.error(e.getMessage(), e);
//                                if (e instanceof NoMeasurementsException) {
//                                    mainthreadWorker.schedule(() -> Toast.makeText(mContext,
//                                            R.string.uploading_track_no_measurements_after_obfuscation_long,
//                                            Toast.LENGTH_LONG).show());
//                                    mNotificationHandler.createNotification
//                                            (mContext.getString(R.string
//                                                    .uploading_track_no_measurements_after_obfuscation));
//                                } else {
//                                    subscriber.onError(e);
//                                }
//                            }
//
//                            @Override
//                            public void onNext(Track track) {
//                                subscriber.onNext(track);
//                            }
//                        }));
            }
        });
    }

    /**
     * Returns an observable that uploads a list of tracks. If a track did not contain enough
     * measurements, i.e. the track obfuscation is throwing a {@link NoMeasurementsException},
     * then it returns null to its subscriber.
     *
     * @param tracks                the list of tracks to upload.
     * @param abortOnNoMeasurements if true, then it also closes the complete stream. Otherwise,
     *                              it returns null to its subscriber.
     * @return an observable that uploads a list of tracks.
     */
    public Observable<Track> uploadTracks(List<Track> tracks, boolean abortOnNoMeasurements) {
        Preconditions.checkState(tracks != null && !tracks.isEmpty(),
                "Input tracks cannot be null or empty.");
        return Observable.from(tracks)
                .concatMap(track -> uploadTrack(track)
                        .first()
                        .lift(new Observable.Operator<Track, Track>() {
                            @Override
                            public Subscriber<? super Track> call(
                                    Subscriber<? super Track> subscriber) {
                                return new Subscriber<Track>() {

                                    @Override
                                    public void onCompleted() {
                                        LOG.info("uploadAllLocalTracks(): onCompleted()");
                                        subscriber.onCompleted();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        LOG.info("onError()");
                                        if (!abortOnNoMeasurements &&
                                                e.getCause() instanceof NoMeasurementsException) {
                                            subscriber.onNext(null);
                                            onCompleted();
                                        } else {
                                            subscriber.onError(e);
                                            unsubscribe();
                                        }
                                    }

                                    @Override
                                    public void onNext(Track track) {
                                        subscriber.onNext(track);
                                    }
                                };
                            }
                        }));
    }

    private Observable<Track> uploadTrack(Track track) {
        return Observable.just(track)
                // Check whether the user is correctly logged in.
                .map(mUserManager.getIsLoggedIn())
                // Update the track metadata.
                .flatMap(track1 -> trackDAOHandler.updateTrackMetadataObservable(track1))
                // Assert whether the track has a temporary car.
                .flatMap(trackMetadata -> mCarManager.assertTemporaryCar(track.getCar()))
                // Set the car reference
                .map(car -> {
                    track.setCar(car);
                    return track;
                })
                // obfuscate the track.
                .map(asObfuscatedTrackWhenChecked())
                // Upload the track
                .flatMap(obfTrack -> mDAOProvider.getTrackDAO().createTrackObservable(obfTrack))
                // Update the database entry
                .flatMap(track1 -> mEnviroCarDB.updateTrackObservable(track1));
    }

    private Func1<Track, Track> validateRequirementsForUpload() {
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track track) {
                if (!track.isLocalTrack()) {
                    String infoText = String.format(mContext.getString(R.string
                            .trackviews_is_already_uploaded), track.getName());
                    LOG.warn(infoText);
                    throw OnErrorThrowable.from(new TrackAlreadyUploadedException(infoText));
                } else if (track.getCar() == null) {
                    String infoText = "Track has no car set. Please delete this track.";
                    LOG.warn(infoText);
                    throw OnErrorThrowable.from(new TrackWithNoValidCarException(infoText));
                } else if (!CarUtils.isCarUploaded(track.getCar())) {
                    String infoText = "Cannot upload tracks with no valid remote car.";
                    LOG.warn(infoText);
                    throw OnErrorThrowable.from(new TrackWithNoValidCarException(infoText));
                } else if (!mUserManager.isLoggedIn()) {
                    String infoText = mContext.getString(R.string.trackviews_not_logged_in);
                    LOG.info(infoText);
                    throw OnErrorThrowable.from(new NotLoggedInException(infoText));
                }
                return track;
            }
        };
    }

    private Func1<Track, Track> asObfuscatedTrackWhenChecked() {
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track track) {
                LOG.info("asObfuscatedTrackWhenChecked()");
                if (PreferencesHandler.isObfuscationEnabled(mContext)) {
                    LOG.info(String.format("obfuscation is enabled. Obfuscating track with %s " +
                            "measurements.", "" + track.getMeasurements().size()));
                    try {
                        return TrackUtils.getObfuscatedTrack(track);
                    } catch (NoMeasurementsException e) {
                        throw OnErrorThrowable.from(e);
                    }
                } else {
                    LOG.info("obfuscation is disabled.");
                    return track;
                }
            }
        };
    }
}
