/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
import org.envirocar.app.exception.GPSOnlyTrackCannotUploadException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.TrackWithNoValidCarException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.TrackUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;


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
    private final UserPreferenceHandler mUserManager;
    private final AgreementManager mAgreementManager;

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
            UserPreferenceHandler userHandler,
            AgreementManager agreementManager) {
        this.mContext = context;
        this.mEnviroCarDB = enviroCarDB;
        this.mCarManager = carPreferenceHandler;
        this.mDAOProvider = daoProvider;
        this.trackDAOHandler = trackDAOHandler;
        this.mUserManager = userHandler;
        this.mAgreementManager = agreementManager;
    }

    /**
     * Returns an observable that uploads a single track.
     *
     * @param track    the track to upload.
     * @param activity
     * @return an observable that uploads a single track.
     */
    public Observable<Track> uploadTrackObservable(Track track, Activity activity) {
        return Observable.create(emitter -> {
            LOG.info("uploadTrackObservable() start uploading.");
//                subscriber.onStart();

            // Create a dialog with which the getUserStatistic can accept the terms of use.
            DisposableObserver disposable = Observable.just(track)
                    // Verify whether the TermsOfUSe have been accepted.
                    // When the TermsOfUse have not been accepted, create an
                    // Dialog to accept and continue when the getUserStatistic has accepted.
                    .compose(AgreementManager.TermsOfUseValidator.create(mAgreementManager, activity))
                    // Continue when the TermsOfUse has been accepted, otherwise
                    // throw an error
                    .flatMap(this::uploadTrack)
                    // Only forward the results to the real subscriber.
                    .subscribeWith(new DisposableObserver<Track>() {
                        @Override
                        public void onNext(Track track) {
                            emitter.onNext(track);
                            emitter.onComplete();
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e);
                            emitter.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            emitter.onComplete();
                        }
                    });

            emitter.setDisposable(new Disposable() {
                @Override
                public void dispose() {
                    disposable.dispose();
                }

                @Override
                public boolean isDisposed() {
                    return disposable.isDisposed();
                }
            });
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
     * accepted for the specific getUserStatistic and the input parameter is not null, then it automatically
     * creates a dialog where the getUserStatistic can accept the terms of use.
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
                .compose(AgreementManager.TermsOfUseValidator.create(mAgreementManager, activity))
                .flatMap(tracks1 -> Observable.fromIterable(tracks1))
                .concatMap(track -> uploadTrack(track));
//                        .lift(getUploadTracksOperator(abortOnNoMeasurements)));
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

    private Function<Track, Track> validateRequirementsForUpload() {
        return track -> {
            if (!track.isLocalTrack()) {
                String infoText = String.format(mContext.getString(R.string
                        .trackviews_is_already_uploaded), track.getName());
                LOG.warn(infoText);
                throw new TrackAlreadyUploadedException(infoText);
            } else if (track.getCar() == null) {
                String infoText = "Track has no car set. Please delete this track.";
                LOG.warn(infoText);
                throw new TrackWithNoValidCarException(infoText);
            } else if (!mUserManager.isLoggedIn()) {
                String infoText = mContext.getString(R.string.trackviews_not_logged_in);
                LOG.info(infoText);
                throw new NotLoggedInException(infoText);
            } else if (!track.hasProperty(Measurement.PropertyKey.SPEED)) {
                String infoText = mContext.getString(R.string.trackviews_cannot_upload_gps_tracks);
                LOG.info(infoText);
                throw new GPSOnlyTrackCannotUploadException(infoText);
            }
            return track;
        };
    }

    private Function<Track, Track> asObfuscatedTrackWhenChecked() {
        return track -> {
            LOG.info("asObfuscatedTrackWhenChecked()");
            if (ApplicationSettings.isObfuscationEnabled(mContext)) {
                LOG.info(String.format("obfuscation is enabled. Obfuscating track with %s " +
                        "measurements.", "" + track.getMeasurements().size()));
                return TrackUtils.getObfuscatedTrack(track);
            } else {
                LOG.info("obfuscation is disabled.");
                return track;
            }
        };
    }

    private ObservableTransformer<Track, Track> validateCarOfTrack() {
        return trackObservable -> trackObservable.flatMap(
                track -> mCarManager
                        .assertTemporaryCar(track.getCar())
                        .map(car -> {
                            track.setCar(car);
                            return track;
                        }));
    }

    private ObservableTransformer<Track, Track> updateTrackMetadata() {
        return trackObservable -> trackObservable.flatMap(
                track -> trackDAOHandler
                        .updateTrackMetadataObservable(track, mUserManager.getUser().getTermsOfUseVersion())
                        .map(trackMetadata -> track));
    }

    private ObservableOperator<Track, Track> getUploadTracksOperator(boolean abortOnNoMeasurements) {
        return observer -> new DisposableObserver<Track>() {

            @Override
            public void onNext(Track track) {

            }

            @Override
            public void onError(Throwable e) {
                LOG.info("onError() Track has not enough measurements to upload.");
                if (!abortOnNoMeasurements && e.getCause() instanceof NoMeasurementsException) {
                    observer.onNext(null);
                    onComplete();
                } else {
                    observer.onError(e);
                    dispose();
                }
            }

            @Override
            public void onComplete() {

            }

        };
    }
}


