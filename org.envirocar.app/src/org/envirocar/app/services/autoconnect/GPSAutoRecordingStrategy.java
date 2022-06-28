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
import java.util.Arrays;
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
        this.pendingIntent = PendingIntent.getBroadcast(service, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        setupActivityTransitions();
        this.disposables.add(
                RxBroadcastReceiver.create(service, new IntentFilter(TRANSITIONS_RECEIVER_ACTION))
                        .doOnNext(this::onReceiveTransitionIntent)
                        .doOnError(LOG::error)
                        .subscribe());
    }

    @Override
    public void stop() {
        removeActivityTransitions();
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

    /**
     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupActivityTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();
        List<Integer> activities = Arrays.asList(
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.WALKING,
                DetectedActivity.STILL,
                DetectedActivity.RUNNING
        );

        for (int activity : activities){
            transitions.add(
                    new ActivityTransition.Builder()
                            .setActivityType(activity)
                            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                            .build());
            transitions.add(
                    new ActivityTransition.Builder()
                            .setActivityType(activity)
                            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                            .build());
        }


        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task = ActivityRecognition.getClient(service).requestActivityTransitionUpdates(request, pendingIntent);
        task.addOnSuccessListener(result -> LOG.info("Transitions Api was successfully registered."));
        task.addOnFailureListener(e -> LOG.warn("Transitions Api could not be registered: " + e, null));
    }

    private void removeActivityTransitions() {
        // Unregister the transitions:
        ActivityRecognition.getClient(service).removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener(aVoid -> LOG.info("Transitions successfully unregistered."))
                .addOnFailureListener(e -> LOG.warn("Transitions could not be unregistered: " + e));
    }
}
