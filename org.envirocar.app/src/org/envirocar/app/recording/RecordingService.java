package org.envirocar.app.recording;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.recording.strategy.OBDRecordingStrategy;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.services.recording.RecordingNotification;
import org.envirocar.app.services.recording.SpeechOutput;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class RecordingService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(RecordingService.class);
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";

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
    private final BroadcastReceiver broadcastReciever = new BroadcastReceiver() {

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

    /**
     * Constructor
     */
    public RecordingService() {
        getLifecycle().addObserver(this.speechOutput);
        getLifecycle().addObserver(this.recordingNotification);
    }

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

        // Register a new BroadcastReceiver that waits for incoming actions issued from the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(broadcastReciever, notificationClickedFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("Starting service with following intent: " + intent);
        super.onStartCommand(intent, flags, startId);

        // Select recording algorithm and start
        this.recordingStrategy = new OBDRecordingStrategy(carPreferences.getCar());
        getLifecycle().addObserver(recordingStrategy);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.info("Destroying RecordingService.");
    }
}
