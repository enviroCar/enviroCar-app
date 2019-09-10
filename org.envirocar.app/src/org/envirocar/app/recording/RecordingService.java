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

import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.app.recording.strategy.OBDRecordingStrategy;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.services.recording.RecordingNotification;
import org.envirocar.app.services.recording.SpeechOutput;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

import java.lang.annotation.Target;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class RecordingService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(RecordingService.class);
    private static final String CHANNEL_ID = "envirocar_recording_channel";

    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    public static RecordingState RECORDING_STATE = RecordingState.RECORDING_STOPPED;

    public static void stopService(Context context) {
        ServiceUtils.stopService(context, RecordingService.class);
    }

    // Injected variables
    @Inject
    protected CarPreferenceHandler carPreferences;
    @Inject
    protected SpeechOutput speechOutput;
    @Inject
    protected RecordingNotification recordingNotification;

    private RecordingStrategy recordingStrategy;

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
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
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

        getLifecycle().addObserver(this.speechOutput);
        getLifecycle().addObserver(this.recordingNotification);

        // Register a new BroadcastReceiver that waits for incoming actions issued from the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(broadcastReceiver, notificationClickedFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("Starting service with following intent: " + intent);
        super.onStartCommand(intent, flags, startId);

        showNotification(ServiceStateForNotification.UNCONNECTED);

        // Select recording algorithm and start
        this.recordingStrategy = new OBDRecordingStrategy(getBaseApplicationComponent(), carPreferences.getCar());
        getLifecycle().addObserver(recordingStrategy);
        recordingStrategy.startRecording(this, recordingState -> {
            RECORDING_STATE = recordingState;
            switch (recordingState) {
                case RECORDING_INIT:
                    showNotification(ServiceStateForNotification.CONNECTING);
                    break;
                case RECORDING_STOPPED:
                    showNotification(ServiceStateForNotification.UNCONNECTED);
                    break;
                case RECORDING_RUNNING:
                    showNotification(ServiceStateForNotification.CONNECTED);
                    break;
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
    }

    private void showNotification(ServiceStateForNotification state) {
        Intent i = new Intent(this, BaseMainActivityBottomBar.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), i, 0);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            channelId = createChannel();
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getBaseContext().getString(state.getTitle()))
                .setContentText(getBaseContext().getString(state.getSubText()))
                .setSmallIcon(state.getIcon())
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        startForeground(181, notification);
    }

    @TargetApi(26)
    private synchronized String createChannel() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "recording notification state", NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);

        if(notificationManager != null){
            notificationManager.createNotificationChannel(channel);
        } else {
            stopSelf();
        }

        return CHANNEL_ID;
    }
}
