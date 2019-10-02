package org.envirocar.app.services.autoconnect;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

/**
 * @author dewall
 */
@AutoRecordingScope
public class GPSAutoRecordingStrategy implements AutoRecordingStrategy {
    private static final Logger LOG = Logger.getLogger(GPSAutoRecordingStrategy.class);
    private static final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    private final AutoRecordingService service;

    private AutoRecordingCallback callback;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PendingIntent pendingIntent;

    @Inject
    public GPSAutoRecordingStrategy(AutoRecordingService service) {
        this.service = service;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {


    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        if (disposables != null) {
            disposables.clear();
        }
    }

    @Override
    public boolean preconditionsFulfilled() {
        return false;
    }

    @Override
    public void run(AutoRecordingCallback callback) {
        this.callback = callback;

        // Activity recognition stuff
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        this.pendingIntent = PendingIntent.getBroadcast(service, 0, intent, 0);
        this.disposables.add(
                RxBroadcastReceiver.create(service, new IntentFilter(TRANSITIONS_RECEIVER_ACTION))
                        .doOnNext(this::onReceiveTransitionIntent)
                        .doOnError(LOG::error)
                        .subscribe());
    }

    @Override
    public void stop() {

    }

    private void onReceiveTransitionIntent(Intent intent) {
        String action = intent.getAction();
        if (TRANSITIONS_RECEIVER_ACTION.equals(action)) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        callback.onRecordingTypeConditionsMet();
                    }
                }
            }
        }
    }

//    /**
//     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
//     * with a custom {@link BroadcastReceiver}
//     */
//    private void setupActivityTransitions() {
//        List<ActivityTransition> transitions = new ArrayList<>();
//
//        transitions.add(
//                new ActivityTransition.Builder()
//                        .setActivityType(DetectedActivity.IN_VEHICLE)
//                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
//                        .build());
//
//        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);
//
//        // Register for Transitions Updates.
//        Task<Void> task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, mPendingIntent);
//        task.addOnSuccessListener(result -> LOG.info("Transitions Api was successfully registered."));
//        task.addOnFailureListener(e -> LOG.warn("Transitions Api could not be registered: " + e, null));
//    }
//
//    private void removeActivityTransitions() {
//        // Unregister the transitions:
//        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
//                .addOnSuccessListener(aVoid -> LOG.info("Transitions successfully unregistered."))
//                .addOnFailureListener(e -> LOG.warn("Transitions could not be unregistered: " + e));
//    }
}
