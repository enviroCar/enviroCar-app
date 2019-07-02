/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import android.app.Activity;
import android.content.Context;

import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.exception.GPSOnlyTrackCannotUploadException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.app.rxutils.ItemForwardSubscriber;
import org.envirocar.app.rxutils.SingleItemForwardSubscriber;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.TrackWithNoValidCarException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.TrackUtils;
import org.envirocar.storage.EnviroCarDB;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
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
    private final CarPreferenceHandler mCarManager;
    private final DAOProvider mDAOProvider;
    private final TrackDAOHandler trackDAOHandler;
    private final UserHandler mUserManager;
    private final TermsOfUseManager mTermsOfUseManager;

    /**
     * Normal constructor for this manager. Specify the context and the dbadapter.
     *
     * @param context the context of the current scope
     */
    @Inject
    public TrackUploadHandler(
            @InjectApplicationScope Context context,
            EnviroCarDB enviroCarDB,
            CarPreferenceHandler carPreferenceHandler,
            DAOProvider daoProvider,
            TrackDAOHandler trackDAOHandler,
            UserHandler userHandler,
            TermsOfUseManager termsOfUseManager) {
        this.mContext = context;
        this.mEnviroCarDB = enviroCarDB;
        this.mCarManager = carPreferenceHandler;
        this.mDAOProvider = daoProvider;
        this.trackDAOHandler = trackDAOHandler;
        this.mUserManager = userHandler;
        this.mTermsOfUseManager = termsOfUseManager;
    }

    /**
     * Returns an observable that uploads a single track.
     *
     * @param track    the track to upload.
     * @param activity
     * @return an observable that uploads a single track.
     */
    public Observable<Track> uploadTrackObservable(Track track, Activity activity) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                LOG.info("uploadTrackObservable() start uploading.");
                subscriber.onStart();

                // Create a dialog with which the user can accept the terms of use.
                subscriber.add(Observable.just(track)
                        // Verify whether the TermsOfUSe have been accepted.
                        // When the TermsOfUse have not been accepted, create an
                        // Dialog to accept and continue when the user has accepted.
                        .compose(TermsOfUseManager.TermsOfUseValidator.create(mTermsOfUseManager,
                                activity))
                        // Continue when the TermsOfUse has been accepted, otherwise
                        // throw an error
                        .flatMap(track1 -> uploadTrack(track1))
                        // Only forward the results to the real subscriber.
                        .subscribe(SingleItemForwardSubscriber.create(subscriber)));
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
    public Observable<Track> uploadTracksObservable(
            List<Track> tracks, boolean abortOnNoMeasurements) {
        return uploadTracksObservable(tracks, abortOnNoMeasurements, null);
    }

    /**
     * Returns an observable that uploads a list of tracks. If a track did not contain enough
     * measurements, i.e. the track obfuscation is throwing a {@link NoMeasurementsException},
     * then it returns null to its subscriber. In case when the terms of use has not been
     * accepted for the specific user and the input parameter is not null, then it automatically
     * creates a dialog where the user can accept the terms of use.
     *
     * @param tracks                the list of tracks to upload.
     * @param abortOnNoMeasurements if true, then it also closes the complete stream. Otherwise,
     *                              it returns null to its subscriber.
     * @param activity              the activity of the current scope. When the activity is not
     *                              null, then it creates a dialog where it can be accepted.
     * @return an observable that uploads a list of tracks.
     */
    public Observable<Track> uploadTracksObservable(
            List<Track> tracks, boolean abortOnNoMeasurements, Activity activity) {
        Preconditions.checkState(tracks != null && !tracks.isEmpty(),
                "Input tracks cannot be null or empty.");
        return Observable.just(tracks)
                .compose(TermsOfUseManager.TermsOfUseValidator.create(mTermsOfUseManager, activity))
                .flatMap(tracks1 -> Observable.from(tracks1))
                .concatMap(track -> uploadTrack(track)
                        .first()
                        .lift(getUploadTracksOperator(abortOnNoMeasurements)));
    }

    private Observable<Track> uploadTrack(Track track) {
        return Observable.just(track)
                // general validation of the track
                .map(validateRequirementsForUpload())
                // assets the car of the track and, in case it is not uploaded, it uploads the
                // car and sets the remoteId
                .compose(validateCarOfTrack())
                // Update the track metadata.
                .compose(updateTrackMetadata())
                // obfuscate the track.
                .map(asObfuscatedTrackWhenChecked())
                // Upload the track
                .flatMap(obfTrack -> mDAOProvider.getTrackDAO().createTrackObservable(obfTrack))
                // Update the database entry
                .flatMap(uploadedTrack -> mEnviroCarDB.updateTrackObservable(uploadedTrack));
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
                } else if (!mUserManager.isLoggedIn()) {
                    String infoText = mContext.getString(R.string.trackviews_not_logged_in);
                    LOG.info(infoText);
                    throw OnErrorThrowable.from(new NotLoggedInException(infoText));
                } else if (!track.hasProperty(Measurement.PropertyKey.SPEED)){
                    String infoText = mContext.getString(R.string.trackviews_cannot_upload_gps_tracks);
                    LOG.info(infoText);
                    throw OnErrorThrowable.from(new GPSOnlyTrackCannotUploadException(infoText));
                }
                return track;
            }
        };
    }

    private Func1<Track, Track> asObfuscatedTrackWhenChecked() {
        return track -> {
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
        };
    }

    private Observable.Transformer<Track, Track> validateCarOfTrack() {
        return trackObservable -> trackObservable.flatMap(
                track -> mCarManager
                        .assertTemporaryCar(track.getCar())
                        .map(car -> {
                            track.setCar(car);
                            return track;
                        }));
    }

    private Observable.Transformer<Track, Track> updateTrackMetadata() {
        return trackObservable -> trackObservable.flatMap(
                track -> trackDAOHandler
                        .updateTrackMetadataObservable(track)
                        .map(trackMetadata -> track));
    }

    private Observable.Operator<Track, Track> getUploadTracksOperator(boolean abortOnNoMeasurements) {
        return subscriber -> new ItemForwardSubscriber<Track>((Subscriber<Track>) subscriber) {
            @Override
            public void onError(Throwable e) {
                LOG.info("onError() Track has not enough measurements to upload.");
                if (!abortOnNoMeasurements && e.getCause() instanceof NoMeasurementsException) {
                    subscriber.onNext(null);
                    onCompleted();
                } else {
                    subscriber.onError(e);
                    unsubscribe();
                }
            }
        };
    }
}


