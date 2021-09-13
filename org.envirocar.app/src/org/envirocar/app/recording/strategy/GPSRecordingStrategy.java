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
package org.envirocar.app.recording.strategy;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.BuildConfig;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.provider.LocationProvider;
import org.envirocar.app.recording.provider.TrackDatabaseSink;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.consumption.LoadBasedEnergyConsumptionAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class GPSRecordingStrategy implements LifecycleObserver, RecordingStrategy {
    private static final Logger LOG = Logger.getLogger(GPSRecordingStrategy.class);
    private static final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    // final injected variables
    private final Context context;
    private final Bus eventBus;
    private final MeasurementProvider measurementProvider;
    private final TrackDatabaseSink trackDatabaseSink;
    private final LocationProvider locationProvider;

    private RecordingListener listener;
    private CompositeDisposable disposables = new CompositeDisposable();

    //
    private LoadBasedEnergyConsumptionAlgorithm energyConsumptionAlgorithm;
    private PendingIntent activityTransitionIntent;
    private GPSOnlyConnectionRecognizer recognizer;

    //
    private int trackTrimDuration = 55 * 2;
    private boolean drivingDetected = false;
    private long startingTime;
    private Disposable stopDrivingFuture;

    /**
     * Constructor.
     */
    public GPSRecordingStrategy(Context context, Bus eventBus, LocationProvider locationProvider, MeasurementProvider measurementProvider,
                                TrackDatabaseSink trackDatabaseSink, CarPreferenceHandler carPreferences) {
        this.context = context;
        this.eventBus = eventBus;
        this.measurementProvider = measurementProvider;
        this.trackDatabaseSink = trackDatabaseSink;
        this.locationProvider = locationProvider;

        // set the car specific properties.
        Car car = carPreferences.getCar();
        this.energyConsumptionAlgorithm = new LoadBasedEnergyConsumptionAlgorithm(car.getFuelType());
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate() {
        LOG.info("Creating GPSRecordingStrategy");

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        LOG.info("Destroying GPSRecordingStrategy");
        this.stopRecording();

        if (disposables != null){
            disposables.clear();
        }

        try {
            eventBus.unregister(measurementProvider);
        } catch (Exception e){
        }

        stopGPSConnectionRecognizer();

        listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
    }

    @Override
    public void startRecording(Service service, RecordingListener listener) {
        this.listener = listener;

//        Intent activityTransitionIntent = new Intent(TRANSITIONS_RECEIVER_ACTION);
//        this.activityTransitionIntent = PendingIntent.getBroadcast(service, 0, activityTransitionIntent, 0);
//
//        IntentFilter intentFilter = new IntentFilter(TRANSITIONS_RECEIVER_ACTION);
//        disposables.add(RxBroadcastReceiver.create(context, intentFilter)
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .compose(checkDrivingState())
//                .compose(receiveMeasurements())
//                .compose(enhanceMeasurements())
//                .compose(trackDatabaseSink.storeInDatabase())
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .doOnDispose(() -> listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED))
//                .subscribeWith(recordingObserver()));

        disposables.add(Observable.just("")
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext(s -> listener.onRecordingStateChanged(RecordingState.RECORDING_RUNNING))
                .compose(receiveMeasurements())
                .compose(enhanceMeasurements())
                .compose(trackDatabaseSink.storeInDatabase())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnDispose(() -> listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED))
                .subscribeWith(recordingObserver()));

        disposables.add(
                locationProvider.startLocating()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.newThread())
                        .subscribe(() -> LOG.info("Completed"), LOG::error));

        /// Init transitions
        this.initTransitionsOfInterest();

        // subscribe for preference changes
        disposables.add(ApplicationSettings.getTrackTrimDurationObservable(context)
                .doOnNext(newDuration -> this.trackTrimDuration = newDuration)
                .subscribe());
    }

    @Override
    public void stopRecording() {
        LOG.info("Stopping the track recording");
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
            disposables = null;
        }

        if (activityTransitionIntent != null) {
            // Unregister transition
            ActivityRecognition.getClient(context)
                    .removeActivityTransitionUpdates(activityTransitionIntent)
                    .addOnSuccessListener(avoid -> LOG.info("Transitions successfully unregistered"))
                    .addOnFailureListener(LOG::error);
            activityTransitionIntent = null;
        }

        stopGPSConnectionRecognizer();
    }


    private ObservableTransformer<Intent, String> checkDrivingState() {
        return upstream -> upstream.flatMap(intent -> Observable.create(new ObservableOnSubscribe<String>() {
            private final Scheduler.Worker stoppingWorker = Schedulers.io().createWorker();

            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                String action = intent.getAction();
                LOG.info("Received broadcast!!! " + action);

                // Check if the extent contained an activity transition.
                if (TRANSITIONS_RECEIVER_ACTION.equals(action) && ActivityTransitionResult.hasResult(intent)) {
                    ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                        LOG.info("Received Broadcast: " + event.getTransitionType() + " " + event.getActivityType());
                        if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            listener.onRecordingStateChanged(RecordingState.RECORDING_RUNNING);
                            drivingDetected = true;
                            eventBus.post(new DrivingDetectedEvent(true));
                            if (stopDrivingFuture != null) {
                                stopDrivingFuture.dispose();
                                stopDrivingFuture = null;
                            }

                            emitter.onNext(action);
                        } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                            eventBus.post(new DrivingDetectedEvent(false));
                            stopDrivingFuture = stoppingWorker.schedule(() -> stopRecording(),
                                    1000 * trackTrimDuration, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            }
        }));
    }

    private ObservableTransformer<String, Measurement> receiveMeasurements() {
        // this is the first access to the measurement objects push it further
        return upstream -> {
            final int samplingRate = ApplicationSettings.getSamplingRate(context) * 1000;
            try {
                eventBus.register(measurementProvider);
            } catch (Exception e) {
            }
            return upstream.flatMap(aString -> measurementProvider.measurements(samplingRate));
        };
    }

    private ObservableTransformer<Measurement, Measurement> enhanceMeasurements() {
        return upstream -> upstream.map(measurement -> {
            LOG.info("Received next recorded measurement.");
            try {
                double consumption = energyConsumptionAlgorithm.calculateConsumption(measurement);
                measurement.setProperty(Measurement.PropertyKey.ENERGY_CONSUMPTION, consumption);
                double co2 = energyConsumptionAlgorithm.calculateCO2FromConsumption(consumption);
                measurement.setProperty(Measurement.PropertyKey.ENERGY_CONSUMPTION_CO2, co2);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
            return measurement;
        });
    }


    private void initTransitionsOfInterest() {
        List<Integer> activities = Arrays.asList(DetectedActivity.IN_VEHICLE, DetectedActivity.STILL);

        List<ActivityTransition> transitions = new ArrayList<>();
        for (int activity : activities) {
            for (int transition :
                    Arrays.asList(ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                            ActivityTransition.ACTIVITY_TRANSITION_EXIT)) {
                ActivityTransition t = new ActivityTransition.Builder()
                        .setActivityType(activity)
                        .setActivityTransition(transition)
                        .build();
                transitions.add(t);
            }
        }

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Task<Void> task = ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, activityTransitionIntent);
        task.addOnSuccessListener(aVoid -> LOG.info("Transition API was successfully registered"));
        task.addOnFailureListener(e -> LOG.error("Error while registering transition api.", e));
    }

    private DisposableObserver<Track> recordingObserver() {
        return new DisposableObserver<Track>() {
            private Track track;

            @Override
            protected void onStart() {
                LOG.info("Starting to hear for activity transitions");
                listener.onRecordingStateChanged(RecordingState.RECORDING_INIT);

                try {
                    if (recognizer != null) {
                        eventBus.unregister(recognizer);
                        recognizer = null;
                    }

                    recognizer = new GPSOnlyConnectionRecognizer();
                    eventBus.register(recognizer);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }

            @Override
            public void onNext(Track track) {
                LOG.info(String.format("Started new Track with ID=%s", track.getTrackID()));
                this.track = track;
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                stopGPSConnectionRecognizer();
                track = null;
            }

            @Override
            public void onComplete() {
                LOG.info("Finished the recording of the track.");
                listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                stopGPSConnectionRecognizer();
                track = null;
            }
        };
    }

    private void stopGPSConnectionRecognizer() {
        try {
            eventBus.unregister(recognizer);
            recognizer = null;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }


    /**
     * Private internal class for detecting
     */
    private final class GPSOnlyConnectionRecognizer {
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;

        private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();
        private Disposable gpsCheckerSubscription;

        private final Runnable gpsConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopRecording();
        };

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (gpsCheckerSubscription != null) {
                gpsCheckerSubscription.dispose();
                gpsCheckerSubscription = null;
            }
            gpsCheckerSubscription = backgroundWorker.schedule(gpsConnectionCloser,
                    GPS_INTERVAL, TimeUnit.MILLISECONDS);
        }

        public void shutDown() {
            LOG.info("shutDown() GPSOnlyConnectionRecognizer");
            if (gpsCheckerSubscription != null) {
                gpsCheckerSubscription.dispose();
                gpsCheckerSubscription = null;
            }
        }
    }
}
