package org.envirocar.app.recording;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.squareup.otto.Bus;

import org.envirocar.app.injection.ScopedBaseInjectorService;
import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.recording.provider.RecordingDetailsProvider;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.recording.notification.RecordingNotification;
import org.envirocar.app.recording.notification.SpeechOutput;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class RecordingService extends ScopedBaseInjectorService {
    private static final Logger LOG = Logger.getLogger(RecordingService.class);
    private static final String CHANNEL_ID = "envirocar_recording_channel";

    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    public static RecordingState RECORDING_STATE = RecordingState.RECORDING_STOPPED;

    public static void stopService(Context context) {
        ServiceUtils.stopService(context, RecordingService.class);
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

    private RecordingStrategy recordingStrategy;
    private RecordingNotification recordingNotification;

    // Broadcast receiver that handles the stopping of the track that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Received action matches the command for stopping the recording process of a track.
            if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                LOG.info("Received Broadcast: Stop Track Recording.");

                // Finish the current track.
                recordingStrategy.stopRecording();
            }
        }
    };

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
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(broadcastReceiver, notificationClickedFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("Starting service with following intent: " + intent);
        super.onStartCommand(intent, flags, startId);

        // Select recording algorithm and start
        this.recordingStrategy = recordingFactory.create();
        getLifecycle().addObserver(recordingStrategy);
        recordingStrategy.startRecording(this, recordingState -> {
            RECORDING_STATE = recordingState;
            bus.post(new RecordingStateEvent(recordingState));

            if(recordingState == RecordingState.RECORDING_STOPPED){
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.info("Destroying RecordingService.");

        if (recordingStrategy != null) {
            recordingStrategy.stopRecording();
            recordingStrategy = null;
        }

        this.unregisterReceiver(broadcastReceiver);
    }
}
