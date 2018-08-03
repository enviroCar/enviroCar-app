package org.envirocar.app.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.notifications.ServiceStateForNotificationForNotification;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class AutomaticGPSTrackService extends BaseInjectorService {
    private static final Logger LOGGER = Logger.getLogger(AutomaticGPSTrackService.class);

    public static final void startService(Context context) {
        ServiceUtils.startService(context, AutomaticGPSTrackService.class);
    }

    public static final void stopService(Context context) {
        ServiceUtils.stopService(context, AutomaticGPSTrackService.class);
    }

    private Scheduler.Worker mWorkerThread = Schedulers.newThread().createWorker();
    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";

    @Inject
    protected CarPreferenceHandler mCarManager;

    private boolean mIsAutoconnect = PreferencesHandler.DEFAULT_BLUETOOTH_AUTOCONNECT;
    private boolean mISAutoStart = true;
    private boolean hasCarSelected = false;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    //Activity recognition stuff
    // The intent action which will be fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private PendingIntent mPendingIntent;

    // Broadcast receiver that handles the different actions that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Received action matches the command for starting the recording of a track.
            if (ACTION_START_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Start Track Recording.");
                startGPSOnlyConnectionService();
            }else if(TRANSITIONS_RECEIVER_ACTION.equals(action)){
                if (ActivityTransitionResult.hasResult(intent)) {
                    ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                        if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                            startGPSOnlyConnectionService();
                        }
                    }
                }
            }
        }
    };


    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        LOGGER.info("onCreate()");
        super.onCreate();

        // Register on the event bus.
        this.bus.register(this);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_START_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, notificationClickedFilter);

        //Activity recognition stuff
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        registerReceiver(mBroadcastReciever, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));


        // Set the Notification
        if (GPSOnlyConnectionService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED) {
                    if (mCarManager.getCar() == null) {
                        NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
                    }else{
                        NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NOT_STARTED);
                    }
        }

        subscriptions.add(
                PreferencesHandler.getSelectedCarObsevable()
                        .map(car -> (car != null))
                        .subscribe(hasCar -> {
                            LOGGER.info(String.format("Received changed selected car -> [%s]",
                                    hasCar));
                            hasCarSelected = hasCar;
                            setAutoConnect();
        }));

        subscriptions.add(
                PreferencesHandler.getGPSAutoconnectObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed autoconnect -> [%s]",
                                    aBoolean));
                            mIsAutoconnect = aBoolean;
                            setAutoConnect();
                        })
        );

        subscriptions.add(
                PreferencesHandler.getGPSBackgroundHandlerEnabledObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed auto start -> [%s]",
                                    aBoolean));
                            mISAutoStart = aBoolean;
                            if(!aBoolean) stopSelf();

                        })
        );
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        mMainThreadWorker.schedule(() -> {
            if(!event.mIsGPSEnabled) stopSelf();
        });
    }

    /**
     * Receiver method for {@link TrackRecordingServiceStateChangedEvent}s posted on the event bus.
     *
     * @param event the corresponding event type.
     */
    @Subscribe
    public void onReceiveTrackRecordingServiceStateChangedEvent(
            TrackRecordingServiceStateChangedEvent event) {
        LOGGER.info(String.format("onReceiveTrackRecordingServiceStateChangedEvent(): %s",
                event.toString()));

        // Update the notification state depending on the event's state.
        switch (event.mState) {
            case SERVICE_STARTING:
                NotificationHandler.closeNotification();
                break;
            case SERVICE_STARTED:
                NotificationHandler.closeNotification();
                break;
            case SERVICE_STOPPING:
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.STOPPING);
                break;
            case SERVICE_STOPPED:
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NOT_STARTED);
                break;

        }
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOGGER.info(String.format("onReceiveNewCarTypeSelectedEvent(): %s", event.toString()));
        hasCarSelected = event.mCar != null;
        if (GPSOnlyConnectionService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED) {
            if (!hasCarSelected) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
            }else{
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NOT_STARTED);
            }
        }
        setAutoConnect();
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();

        // unregister all boradcast receivers.
        unregisterReceiver(mBroadcastReciever);

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }

        this.bus.unregister(this);

        // Close the corresponding notification.
        NotificationHandler.closeNotification();
        removeActivityTransitions();
    }

    private void setAutoConnect(){
        if(hasCarSelected && mIsAutoconnect)  setupActivityTransitions();
        else removeActivityTransitions();
    }

    /**
     * Starts the GPSOnlyConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startGPSOnlyConnectionService() {
        if (!ServiceUtils
                .isServiceRunning(getApplicationContext(), GPSOnlyConnectionService.class)) {

            // Start the GPS Only Connection Service
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), GPSOnlyConnectionService.class));

        }
    }

    /**
     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupActivityTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        LOGGER.info("Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.warn("Transitions Api could not be registered: " + e,null);
                    }
                });
    }

    private void removeActivityTransitions(){
        // Unregister the transitions:
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(aVoid -> LOGGER.info("Transitions successfully unregistered."))
                .addOnFailureListener(e -> LOGGER.warn("Transitions could not be unregistered: " + e));
    }
}
