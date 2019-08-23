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
package org.envirocar.app.services.recording;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.BuildConfig;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class GPSOnlyRecordingService extends AbstractRecordingService {
    private static final Logger LOG = Logger.getLogger(GPSOnlyRecordingService.class);

    public static void stopService(Context context) {
        ServiceUtils.stopService(context, GPSOnlyRecordingService.class);
    }

    private static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    private static final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    public static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STOPPED;

    // Background worker
    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    // config parameters
    private int trackTrimDuration = 55 * 2;
    public static boolean drivingDetected = false;

    private long startingTime;

    // observable subscriptions
    private Disposable trackTrimDurationSubscription;
    private Disposable drivingStoppedSubscription;
    private Disposable measurementSubscription;

    // Parameters for activity recognition
    private PendingIntent activityTransitionPendingIntent;

    private GPSOnlyConnectionRecognizer connectionRecognizer = new GPSOnlyConnectionRecognizer();

    private final Runnable drivingConnectionCloser = () -> {
        LOG.warn("Connection closed to to driving state absence");

        // Finish the current track
        trackRecordingHandler.finishTrackAutomatic();
    };

    /**
     * Handles the intents issued to this service.
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LOG.info("Received broadcast!!! " + action);
            // Check if the intent is intended to stop the recording.
            if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                LOG.info("Received Broadcast: Stop Track Recording.");

                // Finish the current track.
                trackRecordingHandler.finishCurrentTrack();
            }

            // Check if the extent contained an activity transition.
            if (TRANSITIONS_RECEIVER_ACTION.equals(action) && ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

                // handle the transition
                this.handleActivityTransition(context, result);
            }
        }

        private void handleActivityTransition(Context context, ActivityTransitionResult result) {
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                LOG.info("Received Broadcast: " + event.getTransitionType() + " " + event.getActivityType());
                Toast.makeText(context, "Received Broadcast: " + event.getTransitionType() + " " + event.getActivityType(), Toast.LENGTH_LONG).show();
                if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    drivingDetected = true;
                    bus.post(new DrivingDetectedEvent(true));
                    if (drivingStoppedSubscription != null) {
                        drivingStoppedSubscription.dispose();
                        drivingStoppedSubscription = null;
                    }
                } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    bus.post(new DrivingDetectedEvent(false));
                    if (CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STARTED) {
                        drivingStoppedSubscription = backgroundWorker.schedule(drivingConnectionCloser,
                                1000 * trackTrimDuration, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    };


    @Override
    public void onCreate() {
        // Init connection recognizer
        this.connectionRecognizer = new GPSOnlyConnectionRecognizer();
        this.eventBusReceivers.add(this.connectionRecognizer);

        //
        super.onCreate();

        // Initialize activity recognition stuff.
        Intent activityTransitionIntent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        activityTransitionPendingIntent = PendingIntent.getBroadcast(this, 0, activityTransitionIntent, 0);
        registerReceiver(broadcastReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        // Activities to create the transitions for
        List<Integer> activities = Arrays.asList(DetectedActivity.IN_VEHICLE);

        // Initialize transitions
        List<ActivityTransition> transitions = new ArrayList<>();
        for (int activity : activities) {
            ActivityTransition enter = new ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build();

            ActivityTransition exit = new ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build();

            transitions.add(enter);
            transitions.add(exit);
        }

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Task<Void> task = ActivityRecognition
                .getClient(this)
                .requestActivityTransitionUpdates(request, activityTransitionPendingIntent);
        task.addOnSuccessListener(aVoid -> LOG.info("Transition API was successfully registered"));
        task.addOnFailureListener(e -> LOG.error("Error while registering transition api.", e));

        // subscribe for preference changes
        this.trackTrimDurationSubscription =
                PreferencesHandler.getTrackTrimDurationObservable(getApplicationContext())
                        .subscribe(integer -> {
                            LOG.info(String.format("Received changed track trim duration [%s]", integer));
                            trackTrimDuration = integer;
                        });
    }

    @Override
    protected void startRecording() {
        // Start the GPS Recording
        CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STARTED;
        this.bus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STARTED));

        // subscribe for measurements
        this.measurementSubscription = subscribeForMeasurements(getApplicationContext(), this.measurementProvider);

        this.startingTime = SystemClock.elapsedRealtime();
        LOG.info("Setting the custom notification while recording for the first time.");
    }

    @Override
    protected void stopRecording() {
        // Unregister subscriptions.
        this.trackTrimDurationSubscription.dispose();
        this.trackTrimDurationSubscription = null;

        // stopping the GPS Only connection
        this.stopGPSOnlyConnection();

        // Unregister broadcast receivers
        unregisterReceiver(broadcastReceiver);

        // Unregister transitions
        ActivityRecognition.getClient(this)
                .removeActivityTransitionUpdates(activityTransitionPendingIntent)
                .addOnSuccessListener(aVoid -> LOG.info("Transitions successfully unregistered."))
                .addOnFailureListener(e -> LOG.error("Transitions could not be registered", e));

        this.drivingDetected = false;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    private Disposable subscribeForMeasurements(Context context, MeasurementProvider measurementProvider) {
        // this is the first access to the measurement objects push it further
        Long samplingRate = PreferencesHandler.getSamplingRate(context) * 1000;
        return measurementProvider.measurements(samplingRate)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(getMeasurementSubscriber());
    }

    private DisposableObserver<Measurement> getMeasurementSubscriber() {
        return new DisposableObserver<Measurement>() {
            private PublishSubject<Measurement> measurementPublisher = PublishSubject.create();
            private Disposable trackRecordingDisposable = null;

            @Override
            public void onStart() {
                LOG.info("onStart(): MeasuremnetProvider Subscription");
                trackRecordingDisposable = trackRecordingHandler.startNewTrack(measurementPublisher);
            }

            @Override
            public void onComplete() {
                LOG.info("onCompleted(): MeasurementProvider");
                measurementPublisher.onComplete();
                measurementPublisher = null;
                disposeTrackRecording();
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                measurementPublisher.onError(e);
                measurementPublisher = null;
                disposeTrackRecording();
            }

            @Override
            public void onNext(Measurement measurement) {
                LOG.info("onNNNNENEEXT()");
                measurementPublisher.onNext(measurement);
                bus.post(new RecordingNewMeasurementEvent(measurement));
            }

            private void disposeTrackRecording() {
                if (trackRecordingDisposable != null && !trackRecordingDisposable.isDisposed()) {
                    trackRecordingDisposable.dispose();
                }
            }
        };
    }

    /**
     * Method that stops the remoteService, removes everything from the waiting list
     */
    private void stopGPSOnlyConnection() {
        LOG.info("stopGPSOnlyConnection called");
        backgroundWorker.schedule(() -> {
            stopForeground(true);

            if (measurementSubscription != null && !measurementSubscription.isDisposed())
                measurementSubscription.dispose();

            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (trackDetailsProvider != null)
                trackDetailsProvider.clear();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            if (drivingStoppedSubscription != null) {
                drivingStoppedSubscription.dispose();
                drivingStoppedSubscription = null;
            }

            locationHandler.stopLocating();
            this.speechOutput.doTextToSpeech("Device disconnected");

            // Set state of the remoteService to stopped.
            CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STOPPED;
            this.bus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPED));
        });
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
            stopGPSOnlyConnection();
            stopSelf();
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
