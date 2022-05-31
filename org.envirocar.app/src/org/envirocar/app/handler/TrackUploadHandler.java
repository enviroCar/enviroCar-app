/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import com.google.gson.JsonArray;
import org.envirocar.app.R;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.*;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.utils.TrackUtils;
import org.envirocar.core.utils.rx.OptionalOrError;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.ObservableTransformer;
import io.reactivex.Observer;
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
                            if (emitter.isDisposed())
                                return;
                            emitter.onNext(track);
                            emitter.onComplete();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (emitter.isDisposed())
                                return;

                            LOG.error(e);
                            emitter.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            if (emitter.isDisposed())
                                return;
                            emitter.onComplete();
                        }
                    });
            emitter.setDisposable(disposable);
        });
    }

    public Observable<OptionalOrError<Track>> uploadTracksObservable(List<Track> tracks) {
        return uploadTracksObservable(tracks, null);
    }


    /**
     * Returns an observable that uploads a list of tracks. If a track did not contain enough
     * measurements, i.e. the track obfuscation is throwing a {@link NoMeasurementsException},
     * then it returns {@link OptionalOrError} with a {@link TrackUploadException} to its subscriber.
     * In case when the terms of use has not been
     * accepted for the specific getUserStatistic and the input parameter is not null, then it automatically
     * creates a dialog where the getUserStatistic can accept the terms of use.
     *
     * @param tracks   the list of tracks to upload.
     * @param activity the activity of the current scope. When the activity is not
     *                 null, then it creates a dialog where it can be accepted.
     * @return an observable that uploads a list of tracks.
     */
    public Observable<OptionalOrError<Track>> uploadTracksObservable(List<Track> tracks, Activity activity) {
        Preconditions.checkState(tracks != null && !tracks.isEmpty(),
                "Input tracks cannot be null or empty.");
        return Observable.just(tracks)
                .compose(AgreementManager.TermsOfUseValidator.create(mAgreementManager, activity))
                .flatMap(tracks1 -> Observable.fromIterable(tracks1))
                .flatMap(track -> uploadTrack(track)
                        .lift(new OptionalOrErrorMappingOperator()));
    }

    public Track uploadTrackChunkStart(Track track) throws ResourceConflictException, NotConnectedException, DataCreationFailureException, UnauthorizedException {
        track.setTrackStatus(Track.TrackStatus.ONGOING);
        
        // metadata
        TrackMetadata meta = trackDAOHandler.updateTrackMetadataObservable(track, mUserManager.getUser().getTermsOfUseVersion()).blockingFirst();
        track.setMetadata(meta);

        LOG.info("Trying to create track." + track);
        mCarManager
                .assertTemporaryCar(track.getCar());
        return trackDAOHandler.createRemoteTrack(track);
    }

    public void uploadTrackChunk(String remoteID, JsonArray trackFeatures) throws NotConnectedException, UnauthorizedException {
        LOG.info("Trying to update track.");
        trackDAOHandler.updateRemoteTrack(remoteID, trackFeatures);
        LOG.info("Track updated.");
    }

    public void uploadTrackChunkEnd(Track mTrack) throws NotConnectedException, UnauthorizedException {
        LOG.info("Trying to finished track.");
        trackDAOHandler.finishRemoteTrack(mTrack);
        LOG.info("Track finished.");
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
                .flatMap(uploadedTrack -> mEnviroCarDB.updateTrackObservable(uploadedTrack))
                .lift(new UploadExceptionMappingOperator());
    }

    private Function<Track, Track> validateRequirementsForUpload() {
        return track -> {
            if (!track.isLocalTrack()) {
                String infoText = String.format(mContext.getString(R.string
                        .trackviews_is_already_uploaded), track.getName());
                LOG.warn(infoText);
                throw new TrackUploadException(track, TrackUploadException.Reason.TRACK_ALREADY_UPLOADED);
            } else if (track.getCar() == null) {
                String infoText = "Track has no car set. Please delete this track.";
                LOG.warn(infoText);
                throw new TrackUploadException(track, TrackUploadException.Reason.NO_CAR_ASSIGNED);
            } else if (!mUserManager.isLoggedIn()) {
                String infoText = mContext.getString(R.string.trackviews_not_logged_in);
                LOG.info(infoText);
                throw new TrackUploadException(track, TrackUploadException.Reason.NOT_LOGGED_IN);
            }
//            else if (!track.hasProperty(Measurement.PropertyKey.SPEED)) {
//                String infoText = mContext.getString(R.string.trackviews_cannot_upload_gps_tracks);
//                LOG.info(infoText);
//                throw new TrackUploadException(track, TrackUploadException.Reason.GPS_TRACKS_NOT_ALLOWED);
//            }
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
                        .map(trackMetadata -> {
                            // add the metadata to the current track object as well, because this is used in
                            // current upload process
                            track.setMetadata(trackMetadata);
                            return track;
                        }));
    }

    private class UploadExceptionMappingOperator implements ObservableOperator<Track, Track> {
        @Override
        public Observer<? super Track> apply(Observer<? super Track> observer) {
            return new DisposableObserver<Track>() {
                @Override
                public void onNext(Track track) {
                    if (!isDisposed())
                        observer.onNext(track);
                }

                @Override
                public void onError(Throwable e) {
                    LOG.warn(e.getMessage(), e);
                    Throwable result = e;
                    if (e instanceof TrackUploadException) {
                        // do nothing
                    } else if (e instanceof NoMeasurementsException) {
                        result = new TrackUploadException(null, TrackUploadException.Reason.NOT_ENOUGH_MEASUREMENTS);
                    } else {
                        result = new TrackUploadException(null, TrackUploadException.Reason.UNKNOWN);
                    }
                    if (!isDisposed())
                        observer.onError(result);
                }

                @Override
                public void onComplete() {
                    if (!isDisposed())
                        observer.onComplete();
                }
            };
        }
    }

    private class OptionalOrErrorMappingOperator implements ObservableOperator<OptionalOrError<Track>, Track> {
        @Override
        public Observer<? super Track> apply(Observer<? super OptionalOrError<Track>> observer) throws Exception {
            return new DisposableObserver<Track>() {

                @Override
                public void onNext(Track track) {
                    LOG.info("Track '%s' has been successfully uploaded.", track.getDescription());
                    observer.onNext(OptionalOrError.create(track));
                }

                @Override
                public void onError(Throwable e) {
                    LOG.warn(e.getMessage(), e);
                    if (e instanceof TrackUploadException) {
                        TrackUploadException ex = (TrackUploadException) e;
                        LOG.error(String.format("Track not uploaded. Reason -> [%s]", ex.getReason()));
                        observer.onNext(OptionalOrError.create(ex));
                    } else if (e instanceof NoMeasurementsException) {
                        observer.onNext(OptionalOrError.create(new TrackUploadException(
                                null, TrackUploadException.Reason.NOT_ENOUGH_MEASUREMENTS, e)));
                    } else {
                        observer.onNext(OptionalOrError.create(new TrackUploadException(
                                null, TrackUploadException.Reason.UNKNOWN)));
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onComplete() {
                    LOG.info("Finished with uploading tracks");
                    observer.onComplete();
                }
            };
        }
    }
}


