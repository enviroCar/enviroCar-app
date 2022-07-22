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

import android.content.Context;
import android.content.Intent;
import android.icu.text.AlphabeticIndex;

import com.hwangjr.rxbus.Bus;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.R;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.services.trackchunks.TrackchunkUploadService;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.core.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackRecordingHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackRecordingHandler.class);
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
    protected TrackDAOHandler trackDAOHandler;
    @Inject
    protected UserPreferenceHandler mUserManager;
    @Inject
    protected AgreementManager mAgreementManager;
    @Inject
    protected CarPreferenceHandler carHander;

    private Track currentTrack;

    /**
     * Constructor.
     *
     * @param context the context of the activity's scope.
     */
    public TrackRecordingHandler(Context context) {
        // Inject all annotated fields.
        BaseApplication.get(context).getBaseApplicationComponent().inject(this);
    }

    private Observable<Track> createNewDatabaseTrackObservable() {
        return Observable.create(emitter -> {
            String date = format.format(new Date());
            Car car = carHander.getCar();

            Track track = new TrackImpl();
            track.setCar(car);
            track.setName("Track " + date);
            track.setDescription(String.format(
                    mContext.getString(R.string.default_track_description), car
                            != null ? car.getModel() : "null"));

            emitter.onNext(track);
        }).flatMap(track -> mEnvirocarDB.insertTrackObservable((Track) track));
    }

    /**
     * Finishes the current track. On the one hand, the background service that handles the
     * connection to the Bluetooth device gets closed and the track in the database gets finished.
     */
    public void finishCurrentTrack() {
        LOGGER.info("finishCurrentTrack()");
        finishCurrentTrackObservable()
                .doOnError(throwable -> LOGGER.warn(throwable.getMessage(), throwable))
                .blockingGet();
    }

    /**
     * Finishes the current track. On the one hand, the background service that handles the
     * connection to the Bluetooth device gets closed and the track in the database gets finished.
     */
    public Single<Track> finishCurrentTrackObservable() {
        LOGGER.info("finishCurrentTrackObservable()");
        return this.stopRecordingService()
                .andThen(stopTrack())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnSubscribe(disposable -> mBus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPING)))
                .doOnSuccess(track -> mBus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPED)));
    }


    public void finishTrackAutomatic() {
        deleteMeasurementsAutomatic()
                .doOnError(throwable -> LOGGER.warn(throwable.getMessage(), throwable))
                .blockingFirst();

        finishCurrentTrack();
    }

    private Observable<Track> deleteMeasurementsAutomatic() {
        LOGGER.info("deleteMeasurementsAutomatic()");
        return getActiveTrackReference(false)
                .toObservable()
                .flatMap(track -> {
                    if (track == null)
                        return Observable.just(null);
                    long trackTrimDuration = ApplicationSettings.getTrackTrimDurationObservable(mContext).blockingFirst() * 1000;
                    mEnvirocarDB.automaticDeleteMeasurements(System.currentTimeMillis() - trackTrimDuration, track.getTrackID());
                    return mEnvirocarDB.updateTrackObservable(track);
                });

    }

    private Completable stopRecordingService() {
        return Completable.create(emitter -> {
            LOGGER.info("Stopping the recording service.");
            try {
                ServiceUtils.stopService(mContext, RecordingService.class);
                LOGGER.info("Recording services stopped");
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private Single<Track> stopTrack() {
        return getActiveTrackReference(false)
                .flatMap(track -> {
                    LOGGER.info("Trying to stop track");

                    // Fire a new TrackFinishedEvent on the event bus.
//                    mBus.post(new TrackFinishedEvent(currentTrack));
                    LOGGER.info("posted via eventbus");
                    track.setTrackStatus(Track.TrackStatus.FINISHED);

                    LOGGER.info(String.format("Track with local id [%s] successful finished.", track.getTrackID()));
                    currentTrack = null;

                    // Depending on the number of measurements inside the track either update the
                    // database and return the updated reference or delete the database entry.
                    return (track.getMeasurements().size() <= 1) ?
                            mEnvirocarDB.deleteTrackObservable(track).single(track) :
                            mEnvirocarDB.updateTrackObservable(track).single(track);
                });
    }


    /**
     * Returns the most recent track, which is not finished yet. It only returns the track when
     * it has not been finished yet, i.e. its last measurement's position meets the requirements
     * for continuing a track. Otherwise, it sets the track to finished and creates a new database
     * entry when required.
     *
     * @param createNew indicates whether it should create a new track reference when no active
     *                  track is available.
     * @return an observable returning the active track reference.
     */
    private Single<Track> getActiveTrackReference(boolean createNew) {
        LOGGER.info("GetActiveTrack Reference");
        return Single.just(new Optional<>(currentTrack))
                // Is there a current reference? if not, then try to find an instance in the enviroCar database.
                .flatMap(track -> track.isEmpty() ?
                        mEnvirocarDB.getActiveTrackObservable(false)
                                .map(t -> new Optional(t))
                                .singleOrError()
                                .onErrorResumeNext(Single.just(track)) :
                        Single.just(track))

                .flatMap(validateTrackRef(createNew))
                .doOnEvent((track, throwable) -> currentTrack = (Track) track);
    }

    /**
     * This function checks whether the last unfinished track reference is a valid track
     * reference, i.e. if its last measurement's spatial position is not too far away from the
     * current position and the time difference between now and the last measurement is not too
     * large.
     *
     * @param createNew should create a new measurement when it is not matching the requirements.
     * @return a function that validates the requirements.
     */
    private Function<Optional, Single<Track>> validateTrackRef(boolean createNew) {
        return optional -> {
            Track track = (Track) optional.getOptional();
            if (track != null && track.getTrackStatus() == Track.TrackStatus.FINISHED) {

//                try {
                // Check whether the last unfinished track reference is too old to be
                // considered.
                if ((System.currentTimeMillis() - track.getEndTime() < DEFAULT_MAX_TIME_BETWEEN_MEASUREMENTS / 10))
                    return Single.just(track);

                // TODO: Spatial Filtering...

                // trackreference is too old. Set it to finished.
                track.setTrackStatus(Track.TrackStatus.FINISHED);
                mEnvirocarDB.updateTrack(track);
                track = null;
//                } catch (NoMeasurementsException e) {
//                    LOGGER.info("Last unfinished track ref does not contain any measurements." +
//                            " Delete the track");
//
//                    // No Measurements in the last track and it cannot be considered as
//                    // active anymore. Therefore, delete the database entry.
//                    trackDAOHandler.deleteLocalTrack(track);
//                }
            }


            if (track != null) {
                return Single.just(track);
            } else {
                // if there is no current reference cached or in the database, then create a new
                // one and persist it.
                return createNew ? createNewDatabaseTrackObservable().singleOrError() : Single.just(new TrackImpl());
            }
        };
    }
}
