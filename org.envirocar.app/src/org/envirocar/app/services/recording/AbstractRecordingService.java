package org.envirocar.app.services.recording;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.mapbox.mapboxsdk.Mapbox;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 *
 */
public abstract class AbstractRecordingService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(AbstractRecordingService.class);

    // Injectable dependencies.
    @Inject
    protected LocationHandler locationHandler;
    @Inject
    protected TrackDetailsProvider trackDetailsProvider;
    @Inject
    protected PowerManager.WakeLock wakeLock;
    @Inject
    protected MeasurementProvider measurementProvider;
    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;
    @Inject
    protected CarPreferenceHandler carPreferenceHandler;
    @Inject
    protected TrackRecordingHandler trackRecordingHandler;
    @Inject
    protected SpeechOutput speechOutput;
    @Inject
    protected RecordingNotification recordingNotification;


    protected List<Object> eventBusReceivers = new ArrayList<>();


    @Override
    public void onCreate() {
        LOG.info("onCreate()");
        super.onCreate();
        //Mapbox.getInstance(getApplicationContext(), "");


        this.eventBusReceivers.add(this);
//        this.eventBusReceivers.add(this.recordingNotification);
//        this.eventBusReceivers.add(this.speechOutput);
        this.eventBusReceivers.add(this.locationHandler);
        this.eventBusReceivers.add(this.measurementProvider);
        this.eventBusReceivers.add(this.trackDetailsProvider);

        getLifecycle().addObserver(this.speechOutput);
        getLifecycle().addObserver(this.recordingNotification);

        // register on event bus
        for (Object o : eventBusReceivers) {
            this.bus.register(o);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("OnStartCommand: Starting recording service.");
        super.onStartCommand(intent, flags, startId);

        this.speechOutput.doTextToSpeech("Establishing connection");

        // Acquire wake lock for keeping the CPU active.
        this.wakeLock.acquire();

        // Start the location service for location updates.
        this.locationHandler.startLocating();

        // Show a notification
        this.showNotification(ServiceStateForNotification.CONNECTING);

        // Start the recording
        this.startRecording();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister from eventbus
        for (Object o : eventBusReceivers) {
            this.bus.unregister(o);
        }

        this.stopRecording();

        // Stop locating
        this.locationHandler.stopLocating();

        this.speechOutput.doTextToSpeech("Device disconnected.");
    }

    protected void showNotification(ServiceStateForNotification state) {
        Intent i = new Intent(this, BaseMainActivityBottomBar.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), i, 0);

        Notification notification = new NotificationCompat.Builder(this, recordingNotification.CHANNEL_ID)
                .setContentTitle(getBaseContext().getString(state.getTitle()))
                .setContentText(getBaseContext().getString(state.getSubText()))
                .setSmallIcon(state.getIcon())
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        startForeground(181, notification);
    }

    /**
     * @param baseApplicationComponent
     */
    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    /**
     *
     */
    protected abstract void startRecording();


    /**
     *
     */
    protected abstract void stopRecording();

}
