package org.envirocar.app.recording.strategy;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

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
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.provider.TrackDatabaseSink;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.consumption.LoadBasedEnergyConsumptionAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class GPSRecordingStrategy implements LifecycleObserver, RecordingStrategy {
    private static final Logger LOG = Logger.getLogger(OBDRecordingStrategy.class);
    private static final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    @Inject
    @InjectApplicationScope
    protected Context context;
    @Inject
    protected Bus eventBus;
    @Inject
    protected MeasurementProvider measurementProvider;
    @Inject
    protected TrackDatabaseSink trackDatabaseSink;

    private RecordingListener listener;
    private CompositeDisposable disposables = new CompositeDisposable();

    //
    private Car car;
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
     *
     * @param car
     */
    public GPSRecordingStrategy(Car car) {
        this.car = car;
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
    }

    @Override
    public void startRecording(Service service, RecordingListener listener) {
        this.listener = listener;

        Intent activityTransitionIntent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        this.activityTransitionIntent = PendingIntent.getBroadcast(context, 0, activityTransitionIntent, 0);

        IntentFilter intentFilter = new IntentFilter(TRANSITIONS_RECEIVER_ACTION);
        disposables.add(RxBroadcastReceiver.create(context, intentFilter)
                .compose(checkDrivingState())
                .compose(receiveMeasurements())
                .compose(enhanceMeasurements())
                .compose(trackDatabaseSink.storeInDatabase())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnDispose(() -> listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED))
                .subscribeWith(recordingObserver()));

        /// Init transitions
        this.initTransitionsOfInterest();

        // subscribe for preference changes
        disposables.add(PreferencesHandler.getTrackTrimDurationObservable(context)
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
                        Toast.makeText(context, "Received Broadcast: " + event.getTransitionType() + " " + event.getActivityType(), Toast.LENGTH_LONG).show();
                        if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            listener.onRecordingStateChanged(RecordingState.RECORDING_RUNNING);
                            drivingDetected = true;
                            eventBus.post(new DrivingDetectedEvent(true));
                            if (stopDrivingFuture != null) {
                                stopDrivingFuture.dispose();
                                stopDrivingFuture = null;
                            }
                        } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                            eventBus.post(new DrivingDetectedEvent(false));
                            stopDrivingFuture = stoppingWorker.schedule(() -> stopRecording(),
                                    1000 * trackTrimDuration, TimeUnit.MILLISECONDS);
                        }
                    }
                    emitter.onNext(action);
                }
            }

        }));
    }

    private ObservableTransformer<String, Measurement> receiveMeasurements() {
        // this is the first access to the measurement objects push it further
        return upstream -> {
            Long samplingRate = PreferencesHandler.getSamplingRate(context) * 1000;
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
        List<Integer> activities = Arrays.asList(DetectedActivity.IN_VEHICLE);

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
            }

            @Override
            public void onComplete() {
                LOG.info("Finished the recording of the track.");
                listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                stopGPSConnectionRecognizer();
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
