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
package org.envirocar.app.recording;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import com.squareup.otto.Bus;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.injection.ScopedBaseInjectorService;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.recording.notification.RecordingNotification;
import org.envirocar.app.recording.notification.SpeechOutput;
import org.envirocar.app.recording.provider.LocationProvider;
import org.envirocar.app.recording.provider.RecordingDetailsProvider;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.voicecommand.handler.MetadataHandler;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class RecordingService extends ScopedBaseInjectorService {
    private static final Logger LOG = Logger.getLogger(RecordingService.class);
    private static final String CHANNEL_ID = "envirocar_recording_channel";

    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    public static RecordingState RECORDING_STATE = RecordingState.RECORDING_STOPPED;

    public static boolean isRunning(){
        return RECORDING_STATE != RecordingState.RECORDING_STOPPED;
    }

    // Injected variables
    @Inject
    protected SpeechOutput speechOutput;
    @Inject
    protected Bus eventBus;
    @Inject
    protected RecordingDetailsProvider recordingDetailsProvider;
    @Inject
    protected RecordingStrategy.Factory recordingFactory;
    @Inject
    protected LocationProvider locationProvider;
    @Inject
    protected PowerManager.WakeLock wakeLock;

    private RecordingStrategy recordingStrategy;
    private RecordingNotification recordingNotification;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void setupServiceComponent() {
        BaseApplication.get(this)
                .getBaseApplicationComponent()
                .plus(new RecordingModule())
                .inject(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        LOG.info("Creating RecordingService.");
        super.onCreate();

        this.recordingNotification = new RecordingNotification(this, eventBus);

        getLifecycle().addObserver(this.speechOutput);
        getLifecycle().addObserver(this.recordingNotification);
        getLifecycle().addObserver(this.recordingDetailsProvider);

        // Register a new BroadcastReceiver that waits for incoming actions issued from the notification.
        IntentFilter notificationClickedFilter = new IntentFilter(ACTION_STOP_TRACK_RECORDING);
        disposables.add(RxBroadcastReceiver.create(this, notificationClickedFilter)
                .doOnNext(intent -> {
                    String action = intent.getAction();
                    // Received action matches the command for stopping the recording process of a track.
                    if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                        LOG.info("Received Broadcast: Stop Track Recording.");
                        // Finish the current track.
                        recordingStrategy.stopRecording();
                    }
                })
                .doOnError(LOG::error)
                .subscribe());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("Starting service with following intent: " + intent);
        super.onStartCommand(intent, flags, startId);

        // Select recording algorithm and start
        this.recordingStrategy = recordingFactory.create();
        getLifecycle().addObserver(recordingStrategy);
        this.disposables.add(
                locationProvider.startLocating()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.newThread())
                        .doOnDispose(() -> LOG.info("Location Provider has been disposed!"))
                        .subscribe(() -> LOG.info("Completed"), LOG::error));
        this.recordingStrategy.startRecording(this, new RecordingStrategy.RecordingListener() {
            private boolean trackFinished = false;

            @Override
            public void onRecordingStateChanged(RecordingState recordingState) {
                RECORDING_STATE = recordingState;
                bus.post(new RecordingStateEvent(recordingState));

                if (recordingState == RecordingState.RECORDING_STOPPED) {
                    stopSelf();
                }
            }

            @Override
            public void onTrackFinished(Track track) {
                if (!trackFinished) {
                    trackFinished = true;
                    LOG.info("Track has been finished. Throwing TrackFinishedEvent.");
                    bus.post(new TrackFinishedEvent(track));
                }
            }
        });

        wakeLock.acquire();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.info("Destroying RecordingService.");

        if (wakeLock != null && wakeLock.isHeld()){
            wakeLock.release();
        }

        if (recordingStrategy != null) {
            recordingStrategy.stopRecording();
            recordingStrategy = null;
        }

        if (disposables != null) {
            disposables.clear();
        }
    }
}
