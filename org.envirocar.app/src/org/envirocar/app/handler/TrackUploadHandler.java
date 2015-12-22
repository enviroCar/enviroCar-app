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
import android.text.Spanned;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.app.services.NotificationHandler;
import org.envirocar.app.views.MaterialDialogObservable;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;
import org.envirocar.core.utils.CarUtils;
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
public class TrackUploadHandler {
    private static Logger logger = Logger.getLogger(TrackUploadHandler.class);

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
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;

    private final Scheduler.Worker mainthreadWorker = AndroidSchedulers.mainThread().createWorker();

    /**
     * Normal constructor for this manager. Specify the context and the dbadapter.
     *
     * @param ctx the context of the current scope
     */
    public TrackUploadHandler(Context ctx) {
        ((Injector) ctx).injectObjects(this);
    }


    public Observable<Track> uploadSingleTrack2(Track track) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                logger.info("uploadSingleTrack() start uploading.");
                subscriber.onStart();
                try {
                    // Create a dialog with which the user can accept the terms of use.
                    User user = mUserManager.getUser();
                    TermsOfUse currentTermsOfUse = mTermsOfUseManager.getCurrentTermsOfUse();
                    Spanned dialogSpanned = DialogUtil.createTermsOfUseMarkup(currentTermsOfUse,
                            user.getTermsOfUseVersion() == null, mContext);

                    subscriber.add(Observable.just(track)
                            .map(validateRequirementsForUpload())
                            .map(mTermsOfUseManager.verifyTermsOfUse())
                            .flatMap(aBoolean -> aBoolean ? Observable.just(aBoolean) :
                                    MaterialDialogObservable
                                            .createTermsOfUseDialogObservable(mContext,
                                                    dialogSpanned))
                            .flatMap(aBoolean -> aBoolean ? uploadTrack(track) : Observable.error
                                    (new NotAcceptedTermsOfUseException("Not accepted TermsOfUse")))
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
                } catch (ServerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Observable<Track> uploadSingleTrack(final Track track) {
        Preconditions.checkNotNull(track, "Track to upload cannot be null.");
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.onStart();

                subscriber.add(uploadTrack(track)
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

    public Observable<Track> uploadMultipleTracks(List<Track> tracks) {
        Preconditions.checkState(tracks == null || tracks.isEmpty(),
                "Input tracks cannot be null or empty.");
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.onStart();
                mNotificationHandler.createNotification("start");

                subscriber.add(Observable.from(tracks)
                        .concatMap(track -> uploadTrack(track))
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
                                subscriber.onNext(track);
                            }
                        }));
            }
        });
    }

    private Observable<Track> uploadTrack(Track track) {
        return updateTrackMetadataObservable(track)
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

    private Observable<TrackMetadata> updateTrackMetadataObservable(Track track) {
        return Observable.just(track)
                .map(track1 -> new TrackMetadata(Util.getVersionString(mContext),
                        mUserManager.getUser().getTermsOfUseVersion()))
                .flatMap(trackMetadata -> mTrackHandler.updateTrackMetadata(track.getTrackID(),
                        trackMetadata));
    }

    private Func1<Track, Track> validateRequirementsForUpload() {
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track track) {
                if (!track.isLocalTrack()) {
                    String infoText = String.format(mContext.getString(R.string
                            .trackviews_is_already_uploaded), track.getName());
                    logger.info(infoText);
                    throw OnErrorThrowable.from(new TrackAlreadyUploadedException(infoText));
                } else if (track.getCar() == null) {

                } else if (!CarUtils.isCarUploaded(track.getCar())) {
//                    String infoText = mContext.getString(R.string.)
                } else if (!mUserManager.isLoggedIn()) {
                    String infoText = mContext.getString(R.string.trackviews_not_logged_in);
                    logger.info(infoText);
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
                logger.info("asObfuscatedTrackWhenChecked()");
                if (PreferencesHandler.isObfuscationEnabled(mContext)) {
                    logger.info(String.format("obfuscation is enabled. Obfuscating track with %s " +
                            "measurements.", "" + track.getMeasurements().size()));
                    try {
                        return TrackUtils.getObfuscatedTrack(track);
                    } catch (NoMeasurementsException e) {
                        throw OnErrorThrowable.from(e);
                    }
                } else {
                    logger.info("obfuscation is disabled.");
                    return track;
                }
            }
        };
    }
}
