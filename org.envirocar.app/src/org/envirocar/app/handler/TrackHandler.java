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

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.remote.DAOProvider;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * @author de Wall
 */
public class TrackHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackHandler.class);
    private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

    private static final long DEFAULT_MAX_TIME_BETWEEN_MEASUREMENTS = 1000 * 60 * 15;
    private static final double DEFAULT_MAX_DISTANCE_BETWEEN_MEASUREMENTS = 3.0;

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected EnviroCarDB mEnvirocarDB;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected CarPreferenceHandler carHander;

    private BluetoothServiceState mBluetoothServiceState = BluetoothServiceState.SERVICE_STOPPED;


    private Track currentTrack;

    /**
     * Constructor.
     *
     * @param context the context of the activity's scope.
     */
    public TrackHandler(Context context) {
        // Inject all annotated fields.
        ((Injector) context).injectObjects(this);
    }

    public Subscription startNewTrack(PublishSubject<Measurement> publishSubject) {
        return getActiveTrackReference(true)
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.io())
                .subscribe(track -> {
                    publishSubject.subscribe(new Subscriber<Measurement>() {
                        @Override
                        public void onStart() {
                            super.onStart();
                            LOGGER.info("Subscribed on Measurement publisher");
                        }

                        @Override
                        public void onCompleted() {
                            LOGGER.info("NewMeasurementSubject onCompleted()");
                            finishTrack(track);
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOGGER.error(e.getMessage(), e);
                            finishTrack(track);
                        }

                        @Override
                        public void onNext(Measurement measurement) {
                            LOGGER.info("onNextMeasurement()");
                            if (isUnsubscribed())
                                return;
                            LOGGER.info("Insert new measurement ");

                            // set the track database ID of the current active track
                            measurement.setTrackId(track.getTrackID());
                            track.getMeasurements().add(measurement);
                            try {
                                mEnvirocarDB.insertMeasurement(measurement);
                            } catch (MeasurementSerializationException e) {
                                LOGGER.error(e.getMessage(), e);
                                finishTrack(track);
                            }
                        }
                    });
                });
    }

    private void finishTrack(Track track) {
        LOGGER.info(String.format("finishTrack(%s)", track.getTrackID()));
        track.setTrackStatus(Track.TrackStatus.FINISHED);
        mEnvirocarDB.updateTrack(track);
        currentTrack = null;
    }

    private Observable<Track> getActiveTrackReference(boolean createNew) {
        return Observable.just(currentTrack)
                // Is there a current reference? if not, then try to find an instance in the
                // enviroCar database.
                .flatMap(track -> track == null ?
                        mEnvirocarDB.getActiveTrackObservable(false) : Observable.just(track))
                .flatMap(validateTrackRef(createNew))
                // Optimize it....
                .map(track -> {
                    currentTrack = track;
                    return track;
                });
    }

    private Func1<Track, Observable<Track>> validateTrackRef(boolean createNew) {
        return new Func1<Track, Observable<Track>>() {
            @Override
            public Observable<Track> call(Track track) {
                if (track != null && track.getTrackStatus() == Track.TrackStatus.FINISHED) {
                    try {
                        // Check whether the last unfinished track reference is too old to be
                        // considered.
                        if ((System.currentTimeMillis() - track.getEndTime() <
                                DEFAULT_MAX_TIME_BETWEEN_MEASUREMENTS / 10))
                            return Observable.just(track);

                        // TODO: Spatial Filtering...

                        // trackreference is too old. Set it to finished.
                        track.setTrackStatus(Track.TrackStatus.FINISHED);
                        mEnvirocarDB.updateTrack(track);
                    } catch (NoMeasurementsException e) {
                        LOGGER.info("Last unfinished track ref does not contain any measurements." +
                                " Delete the track");

                        // No Measurements in the last track and it cannot be considered as
                        // active anymore. Therefore, delete the database entry.
                        deleteLocalTrack(track);
                    }
                }


                if (track != null) {
                    return Observable.just(track);
                } else {
                    // if there is no current reference cached or in the database, then create a new
                    // one and persist it.
                    return createNew ? createNewDatabaseTrackObservable() : Observable.just(null);
                }
            }
        };
    }

    private Observable<Track> createNewDatabaseTrackObservable() {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                String date = format.format(new Date());
                Car car = carHander.getCar();

                Track track = new TrackImpl();
                track.setCar(car);
                track.setName("Track " + date);
                track.setDescription(String.format(
                        mContext.getString(R.string.default_track_description), car
                                != null ? car.getModel() : "null"));

                subscriber.onNext(track);
            }
        }).flatMap(track -> mEnvirocarDB.insertTrackObservable(track));
    }

    /**
     * Deletes a track and returns true if the track has been successfully deleted.
     *
     * @param trackID the id of the track to delete.
     * @return true if the track has been successfully deleted.
     */
    public boolean deleteLocalTrack(Track.TrackId trackID) {
        return deleteLocalTrack(
                mEnvirocarDB.getTrack(trackID)
                        .subscribeOn(Schedulers.io())
                        .toBlocking()
                        .first());
    }

    /**
     * Deletes a track and returns true if the track has been successfully deleted.
     *
     * @param trackRef the reference of the track.
     * @return true if the track has been successfully deleted.
     */
    public boolean deleteLocalTrack(Track trackRef) {
        LOGGER.info(String.format("deleteLocalTrack(id = %s)", trackRef.getTrackID().getId()));

        // Only delete the track if the track is a local track.
        if (trackRef.isLocalTrack()) {
            LOGGER.info("deleteLocalTrack(...): Track is a local track.");
            mEnvirocarDB.deleteTrack(trackRef);
            return true;
        }

        LOGGER.warn("deleteLocalTrack(...): track is no local track. No deletion.");
        return false;
    }

    /**
     * Invokes the deletion of a remote track. Once the remote track has been successfully
     * deleted, this method also deletes the locally stored reference of that track.
     *
     * @param trackRef
     * @return
     * @throws UnauthorizedException
     * @throws NotConnectedException
     */
    public boolean deleteRemoteTrack(Track trackRef) throws UnauthorizedException,
            NotConnectedException {
        LOGGER.info(String.format("deleteRemoteTrack(id = %s)", trackRef.getTrackID().getId()));

        // Check whether this track is a remote track.
        if (!trackRef.isRemoteTrack()) {
            LOGGER.warn("Track reference to upload is no remote track.");
            return false;
        }

        // Delete the track first remote and then the local reference.
        try {
            mDAOProvider.getTrackDAO().deleteTrack(trackRef);
        } catch (DataUpdateFailureException e) {
            e.printStackTrace();
        }

        mEnvirocarDB.deleteTrack(trackRef);

        // Successfully deleted the remote track.
        LOGGER.info("deleteRemoteTrack(): Successfully deleted the remote track.");
        return true;
    }

    public boolean deleteAllRemoteTracksLocally() {
        LOGGER.info("deleteAllRemoteTracksLocally()");
        mEnvirocarDB.deleteAllRemoteTracks()
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .first();
        return true;
    }


    public Observable<TrackMetadata> updateTrackMetadata(
            Track.TrackId trackId, TrackMetadata trackMetadata) {
        return mEnvirocarDB.getTrack(trackId, true)
                .map(track -> {
                    TrackMetadata result = track.updateMetadata(trackMetadata);
                    mEnvirocarDB.updateTrack(track);
                    return result;
                });
    }

    private boolean assertIsUserLoggedIn() throws NotLoggedInException {
        if (mUserManager.isLoggedIn()) {
            return true;
        } else {
            throw new NotLoggedInException("Not Logged In");
        }
    }

    private BlockingObservable<Boolean> asserHasAcceptedTermsOfUseObservable() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                // First, try to get whether the user has accepted the terms of use.
                final User user = mUserManager.getUser();
                boolean verified = false;
                try {
                    verified = mTermsOfUseManager.verifyTermsUseOfVersion(user
                            .getTermsOfUseVersion());
                } catch (ServerException e) {
                    LOGGER.warn(e.getMessage(), e);
                    String infoText = mContext.getString(R.string.trackviews_server_error);
                    subscriber.onError(new NotAcceptedTermsOfUseException(infoText));
                }
            }
        }).toBlocking();
    }

    private boolean assertHasAcceptedTermsOfUse() throws NotAcceptedTermsOfUseException {
        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            throw new NotAcceptedTermsOfUseException(infoText);
        }

        return verified;
    }

    private boolean assertHasAcceptedTermsOfUse(Subscriber<? super Track> subscriber) {
        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            subscriber.onError(e);
            return false;
        }

        return verified;
    }

    private boolean assertIsLocalTrack(Track track, Subscriber<? super Track> subscriber) {
        // If the track is no local track, then popup a snackbar.
        if (!track.isLocalTrack()) {
            String infoText = String.format(mContext.getString(R.string
                    .trackviews_is_already_uploaded), track.getName());
            LOGGER.info(infoText);
            subscriber.onError(new TrackAlreadyUploadedException(infoText));
            return false;
        }
        return true;
    }

    private boolean assertIsUserLoggedIn(Subscriber<? super Track> subscriber) {
        // If the user is not logged in, then skip the upload and popup a snackbar.
        if (!mUserManager.isLoggedIn()) {
            LOGGER.warn("Cannot upload track, because the user is not logged in");
            String infoText = mContext.getString(R.string.trackviews_not_logged_in);
            subscriber.onError(new NotLoggedInException(infoText));
            return false;
        }
        return true;
    }

    public Observable<Track> fetchRemoteTrackObservable(Track remoteTrack) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                try {
                    subscriber.onNext(fetchRemoteTrack(remoteTrack));
                    subscriber.onCompleted();
                } catch (NotConnectedException e) {
                    throw OnErrorThrowable.from(e);
                } catch (DataRetrievalFailureException e) {
                    throw OnErrorThrowable.from(e);
                } catch (UnauthorizedException e) {
                    throw OnErrorThrowable.from(e);
                }
            }
        });
    }

    public Track fetchRemoteTrack(Track remoteTrack) throws NotConnectedException,
            UnauthorizedException, DataRetrievalFailureException {
        try {
            Track downloadedTrack = mDAOProvider.getTrackDAO().getTrackById(remoteTrack
                    .getRemoteID());

            // Deep copy... TODO improve this.
            remoteTrack.setName(downloadedTrack.getName());
            remoteTrack.setDescription(downloadedTrack.getDescription());
            remoteTrack.setMeasurements(new ArrayList<>(downloadedTrack.getMeasurements()));
            remoteTrack.setCar(downloadedTrack.getCar());
            remoteTrack.setTrackStatus(downloadedTrack.getTrackStatus());
            remoteTrack.setMetadata(downloadedTrack.getMetadata());

            remoteTrack.setStartTime(downloadedTrack.getStartTime());
            remoteTrack.setEndTime(downloadedTrack.getEndTime());
            remoteTrack.setDownloadState(Track.DownloadState.DOWNLOADED);
        } catch (NoMeasurementsException e) {
            e.printStackTrace();
        }

        try {
            mEnvirocarDB.insertTrack(remoteTrack);
        } catch (TrackSerializationException e) {
            e.printStackTrace();
        }
        //        mDBAdapter.insertTrack(remoteTrack, true);
        return remoteTrack;
    }

    /**
     * Finishes the current track. On the one hand, the remoteService that handles the connection to
     * the Bluetooth device gets closed and the track in the database gets finished.
     */
    public void finishCurrentTrack() {
        LOGGER.info("stopTrack()");

        // Set the current remoteService state to SERVICE_STOPPING.
        mBus.post(new BluetoothServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPING));

        // Schedule a new async task for closing the remoteService, finishing the current track, and
        // finally fireing an event on the event bus.

        Track track = getActiveTrackReference(false)
                .map(track1 -> {
                    // Stop the background remoteService that is responsible for the
                    // OBDConnection.
                    mBluetoothHandler.stopOBDConnectionService();

                    if (track1 != null) {
                        track1.setTrackStatus(Track.TrackStatus.FINISHED);
                        mEnvirocarDB.updateTrack(track1);

                        // Fire a new TrackFinishedEvent on the event bus.
                        mBus.post(new TrackFinishedEvent(track1));
                    }

                    return track1;
                })
                .toBlocking()
                .first();
        currentTrack = null;

        LOGGER.info(String.format("Track with local id [%s] successful finished.",
                track.getTrackID()));
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("onReceiveBluetoothServiceStateChangedEvent: %s",
                event.toString()));
        mBluetoothServiceState = event.mState;
    }

}
