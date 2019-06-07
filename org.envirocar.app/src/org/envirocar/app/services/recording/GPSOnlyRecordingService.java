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

import org.envirocar.app.BuildConfig;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.services.GPSOnlyConnectionService;
import org.envirocar.app.views.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class GPSOnlyRecordingService extends AbstractRecordingService {
    private static final Logger LOG = Logger.getLogger(GPSOnlyRecordingService.class);

    private static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    private static final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    private static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STOPPED;

    // Background worker
    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    // config parameters
    private int trackTrimDuration = 55 * 2;
    private boolean drivingDetected = false;

    // observable subscriptions
    private Subscription trackTrimDurationSubscription;
    private Subscription drivingStoppedSubscription;
    private Subscription measurementSubscription;

    // Parameters for activity recognition
    private PendingIntent activityTransitionPendingIntent;

    private GPSOnlyConnectionService.GPSOnlyConnectionRecognizer connectionRecognizer = new GPSOnlyConnectionService.GPSOnlyConnectionRecognizer();

    private final Action0 drivingConnectionCloser = () -> {
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
                        drivingStoppedSubscription.unsubscribe();
                        drivingStoppedSubscription = null;
                    }
                } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    bus.post(new DrivingDetectedEvent(false));
                    if (CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STARTED) {
                        drivingStoppedSubscription = backgroundWorker.schedule(
                                drivingConnectionCloser, 1000 * trackTrimDuration, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize activity recognition stuff.
        Intent activityTransitionIntent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        activityTransitionPendingIntent = PendingIntent.getBroadcast(this, 0, activityTransitionIntent, 0);
        registerReceiver(broadcastReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        // Initialize transition
        List<ActivityTransition> transitions = Arrays.asList(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build(),
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

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
                            LOG.info(String.format("Received changed track trim duration [%s]", integer);
                            trackTrimDuration = integer;
                        });
    }

    @Override
    protected void startRecording() {
        // Start the GPS Recording
        CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STARTED;
        this.bus.post(new TrackRecordingServiceStateChangedEvent(BluetoothServiceState.SERVICE_STARTED));

        subscribeForMeasurements();
        mStartingTime = SystemClock.elapsedRealtime();
        LOG.info("Setting the custom notification while recording for the first time.");
        refreshNotification();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop the GPS Only Connection

        // Unregister subscriptions.
        this.trackTrimDurationSubscription.unsubscribe();
        this.trackTrimDurationSubscription = null;

        // Unregister broadcast receivers
        unregisterReceiver(broadcastReceiver);

        // Unregister transitions
        ActivityRecognition.getClient(this)
                .removeActivityTransitionUpdates(activityTransitionPendingIntent)
                .addOnSuccessListener(aVoid -> LOG.info("Transitions successfully unregistered."))
                .addOnFailureListener(e -> LOG.error("Transitions could not be registered", e));

        this.drivingDetected = false;

        LOG.info("GPSOnlyRecordingService successfully destroyed");
    }

    @Override
    protected Class<? extends BaseInjectorActivity> getRecordingScreenClass() {
        return GPSOnlyTrackRecordingScreen.class;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    private void subscribeForMeasurements() {
        // this is the first access to the measurement objects push it further
        Long samplingRate = PreferencesHandler.getSamplingRate(getApplicationContext()) * 1000;
        measurementSubscription = measurementProvider.measurements(samplingRate)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(getMeasurementSubscriber());
    }

    private Subscriber<Measurement> getMeasurementSubscriber() {
        return new Subscriber<Measurement>() {
            PublishSubject<Measurement> measurementPublisher =
                    PublishSubject.create();

            @Override
            public void onStart() {
                LOG.info("onStart(): MeasuremnetProvider Subscription");
                add(trackRecordingHandler.startNewTrack(measurementPublisher));
            }

            @Override
            public void onCompleted() {
                LOG.info("onCompleted(): MeasurementProvider");
                measurementPublisher.onCompleted();
                measurementPublisher = null;
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                measurementPublisher.onError(e);
                measurementPublisher = null;
            }

            @Override
            public void onNext(Measurement measurement) {
                LOG.info("onNNNNENEEXT()");
                measurementPublisher.onNext(measurement);
                bus.post(new RecordingNewMeasurementEvent(measurement));
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

            if (measurementSubscription != null && !measurementSubscription.isUnsubscribed())
                measurementSubscription.unsubscribe();

            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (mTrackDetailsProvider != null)
                mTrackDetailsProvider.clear();
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            if (mDrivingStoppedSubscription != null) {
                mDrivingStoppedSubscription.unsubscribe();
                mDrivingStoppedSubscription = null;
            }

            mLocationHandler.stopLocating();
            doTextToSpeech("Device disconnected");

            // Set state of the remoteService to stopped.
            setTrackRecordingServiceState(BluetoothServiceState.SERVICE_STOPPED);
        });
    }


    /**
     * Private internal class for detecting
     */
    private final class GPSOnlyConnectionRecognizer {
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;

        private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();
        private Subscription gpsCheckerSubscription;

        private final Action0 gpsConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopGPSOnlyConnection();
            stopSelf();
        };

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (gpsCheckerSubscription != null) {
                gpsCheckerSubscription.unsubscribe();
                gpsCheckerSubscription = null;
            }
            gpsCheckerSubscription = backgroundWorker.schedule(
                    gpsConnectionCloser, GPS_INTERVAL, TimeUnit.MILLISECONDS);
        }

        public void shutDown() {
            LOG.info("shutDown() GPSOnlyConnectionRecognizer");
            if (gpsCheckerSubscription != null) {
                gpsCheckerSubscription.unsubscribe();
            }
        }
    }
}
