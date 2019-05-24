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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.R;
import org.envirocar.app.services.GPSOnlyConnectionService;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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
    protected UserHandler mUserManager;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
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

    public Subscription startNewTrack(PublishSubject<Measurement> publishSubject) {
        return getActiveTrackReference(true)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<Track>() {
                    @Override
                    public void onCompleted() {
                        LOGGER.info("onCompleted()");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Track track) {
                        add(publishSubject.doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                LOGGER.info("doOnUnsubscribe(): finish current track.");
                                finishCurrentTrack();
                            }
                        }).subscribe(new Subscriber
                                <Measurement>() {
                            @Override
                            public void onStart() {
                                super.onStart();
                                LOGGER.info("Subscribed on Measurement publisher");
                            }

                            @Override
                            public void onCompleted() {
                                LOGGER.info("NewMeasurementSubject onCompleted()");
                                currentTrack = track;
                                finishCurrentTrack();
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                                currentTrack = track;
                                finishCurrentTrack();
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
                                currentTrack = track;
                                try {
                                    mEnvirocarDB.insertMeasurement(measurement);
                                } catch (MeasurementSerializationException e) {
                                    LOGGER.error(e.getMessage(), e);
                                    finishCurrentTrack();
                                }
                            }
                        }));
                    }
                });
    }

    /**
     * Returns the most recent track, which is not finished yet. It only returns the track when
     * it has not been finished yet, i.e. its last measurement'S position meets the requirements
     * for continuing a track. Otherwise, it sets the track to finished and creates a new database
     * entry when required.
     *
     * @param createNew indicates whether it should create a new track reference when no active
     *                  track is available.
     * @return an observable returning the active track reference.
     */
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

    /**
     * This function checks whether the last unfinished track reference is a valid track
     * reference, i.e. if its last measurement's spatial position is no to far away from the
     * current position and the time difference between now and the last measurement is not to
     * large.
     *
     * @param createNew should create a new measurement when it is not matching the requirements.
     * @return a function that validates the requirements.
     */
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
                        track = null;
                    } catch (NoMeasurementsException e) {
                        LOGGER.info("Last unfinished track ref does not contain any measurements." +
                                " Delete the track");

                        // No Measurements in the last track and it cannot be considered as
                        // active anymore. Therefore, delete the database entry.
                        trackDAOHandler.deleteLocalTrack(track);
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
     * Finishes the current track. On the one hand, the background service that handles the
     * connection to the Bluetooth device gets closed and the track in the database gets finished.
     */
    public void finishCurrentTrack() {
        LOGGER.info("finishCurrentTrack()");
        finishCurrentTrackObservable()
                .doOnError(throwable -> LOGGER.warn(throwable.getMessage(), throwable))
                .toBlocking()
                .first();
    }

    /**
     * Finishes the current track. On the one hand, the background service that handles the
     * connection to the Bluetooth device gets closed and the track in the database gets finished.
     */
    public Observable<Track> finishCurrentTrackObservable() {
        LOGGER.info("finishCurrentTrackObservable()");

        // Set the current remoteService state to SERVICE_STOPPING.
        mBus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPING));

        return getActiveTrackReference(false)
                .flatMap(track -> {
                    // Stop the background service.
                    stopBackgroundRecordingServices();

                    if (track == null)
                        return Observable.just(track);

                    // Fire a new TrackFinishedEvent on the event bus.
                    mBus.post(new TrackFinishedEvent(currentTrack));
                    track.setTrackStatus(Track.TrackStatus.FINISHED);

                    LOGGER.info(String.format("Track with local id [%s] successful " +
                            "finished.", track.getTrackID()));
                    currentTrack = null;

                    // Depending on the number of measurements inside the track either update the
                    // database and return the updated reference or delete the database entry.
                    return (track.getMeasurements().size() <= 1) ?
                            mEnvirocarDB.deleteTrackObservable(track) :
                            mEnvirocarDB.updateTrackObservable(track);
                });
    }

    public void finishTrackAutomatic(){
        deleteMeasurementsAutomatic()
                .doOnError(throwable -> LOGGER.warn(throwable.getMessage(), throwable))
                .toBlocking()
                .first();

        finishCurrentTrack();
    }

    private Observable<Track> deleteMeasurementsAutomatic(){
        LOGGER.info("deleteMeasurementsAutomatic()");
        return getActiveTrackReference(false)
                .flatMap(track -> {
                    if (track == null)
                        return Observable.just(null);
                    long trackTrimDuration = PreferencesHandler.getTrackTrimDuration(mContext)*1000;
                    mEnvirocarDB.automaticDeleteMeasurements(System.currentTimeMillis() - trackTrimDuration , track.getTrackID());
                    return mEnvirocarDB.updateTrackObservable(track);
                });

    }

    public void stopBackgroundRecordingServices() {
        LOGGER.info("stopBackgroundRecordingServices()");
        if (ServiceUtils.isServiceRunning(mContext, OBDConnectionService.class)) {
            mContext.getApplicationContext()
                    .stopService(new Intent(mContext, OBDConnectionService.class));
        }

        if (ServiceUtils.isServiceRunning(mContext, GPSOnlyConnectionService.class)) {
            mContext.getApplicationContext()
                    .stopService(new Intent(mContext, GPSOnlyConnectionService.class));
        }

        ActivityManager amgr = (ActivityManager) mContext.getSystemService(Context
                .ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> list = amgr.getRunningAppProcesses();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ActivityManager.RunningAppProcessInfo apinfo = list.get(i);

                String[] pkgList = apinfo.pkgList;
                if (apinfo.processName.startsWith("org.envirocar.app.services.OBD")) {
                    for (int j = 0; j < pkgList.length; j++) {
                        amgr.killBackgroundProcesses(pkgList[j]);
                    }
                }
            }
        }
    }
}
