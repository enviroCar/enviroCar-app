/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.services;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

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
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.app.services.recording.GPSOnlyRecordingService;
import org.envirocar.app.services.recording.OBDRecordingService;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observers.SafeSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.envirocar.app.notifications.NotificationHandler.context;

public class AutomaticTrackRecordingService extends BaseInjectorService {
    private static final Logger LOGGER = Logger.getLogger(AutomaticTrackRecordingService.class);

    public static final void startService(Context context) {
        ServiceUtils.startService(context, AutomaticTrackRecordingService.class);
    }

    public static final void stopService(Context context) {
        ServiceUtils.stopService(context, AutomaticTrackRecordingService.class);
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Scheduler.Worker mWorkerThread = Schedulers.newThread().createWorker();
    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private static final int REDISCOVERY_INTERVAL = 30;

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";

    // Injected variables
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected LocationHandler mLocationHandler;

    //the main driver which switches the autoconnect settings of OBD and GPS tracks.
    private int recordingTypeSelected = 1;

    private boolean mIsAutoconnect = PreferencesHandler.DEFAULT_BLUETOOTH_AUTOCONNECT;
    private boolean hasCarSelected = false;
    private int mDiscoveryInterval = PreferencesHandler.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL;

    // private member fields.
    private Subscription mWorkerSubscription;
    private Subscription mDiscoverySubscription;
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

            // Received action matches the command for starting the discovery process for the
            // selected OBDII-Adapter.
            if (ACTION_START_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Start Discovery.");

                // If the bluetooth is currently disabled, then do not issue the discovery.
                if (!mBluetoothHandler.isBluetoothEnabled()) {
                    LOGGER.severe("Bluetooth is disabled. No Bluetooth discovery is issued");
                }

                startDiscoveryForSelectedDevice();
            }

            // Received action matches the command for stopping the Bluetooth discovery process.
            else if (ACTION_STOP_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Stop Discovery.");

                mBluetoothHandler.stopBluetoothDeviceDiscovery();

                // Set the notification state to unconnected.
                NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);

                // UNUSED: This leads sometimes to some errors if you always ski
                if (mDiscoverySubscription != null) {
                    mDiscoverySubscription.unsubscribe();
                    mDiscoverySubscription = null;
                }
            }

            // Received action matches the command for starting the recording of a track.
            else if (ACTION_START_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Start Track Recording.");

                if(recordingTypeSelected == 1) startOBDConnectionService();
                else startGPSOnlyConnectionService();

            }else if(TRANSITIONS_RECEIVER_ACTION.equals(action)){
                if (ActivityTransitionResult.hasResult(intent) && recordingTypeSelected == 2) {
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

        // Get the required preference settings.
        this.mDiscoveryInterval = PreferencesHandler.getDiscoveryInterval(context);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_START_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_STOP_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_START_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, notificationClickedFilter);

        //Activity recognition stuff
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        registerReceiver(mBroadcastReciever, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));


        subscriptions.add(
                PreferencesHandler.getSelectedCarObsevable()
                        .map(car -> (car != null))
                        .subscribe(hasCar -> {
                            LOGGER.info(String.format("Received changed selected car -> [%s]",
                                    hasCar));

                            hasCarSelected = hasCar;

                            if(recordingTypeSelected == 1){
                                if(hasCarSelected){
                                    scheduleDiscovery(mDiscoveryInterval);
                                }else{
                                    unscheduleDiscovery();
                                }
                            }else {
                                setGPSAutoConnect();
                            }
                        }));

        subscriptions.add(
                PreferencesHandler.getDiscoveryIntervalObservable(getApplicationContext())
                        .subscribe(integer -> {
                            LOGGER.info(String.format("Received changed discovery interval -> [%s]",
                                    integer));
                            mDiscoveryInterval = integer;
                            if(recordingTypeSelected == 1) scheduleDiscovery(mDiscoveryInterval);
                        })
        );

        subscriptions.add(
                PreferencesHandler.getAutoconnectObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed autoconnect -> [%s]",
                                    aBoolean));
                            mIsAutoconnect = aBoolean;
                            if(recordingTypeSelected == 1)  scheduleDiscovery(mDiscoveryInterval);
                            else setGPSAutoConnect();
                        })
        );

        subscriptions.add(
                PreferencesHandler.getBackgroundHandlerEnabledObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed autostart -> [%s]",
                                    aBoolean));
                            if(!aBoolean) stopSelf();

                        })
        );

        subscriptions.add(
                PreferencesHandler.getPreviouslySelectedRecordingTypeObservable(getApplicationContext())
                        .subscribe(recType -> {
                            LOGGER.info(String.format("Received changed recording type -> [%s]",
                                    recType));
                            recordingTypeSelected = recType;

                            if(recordingTypeSelected == 1){
                                stopAutomaticGPSTrackRecordingProcedures();
                                startAutomaticOBDTrackRecordingProcedures();
                            }else{
                                stopAutomaticOBDTrackRecordingProcedures();
                                startAutomaticGPSTrackRecordingProcedures();
                            }

                        })
        );

        if(recordingTypeSelected == 1){
            //OBD Track recording type set
            startAutomaticOBDTrackRecordingProcedures();
        }else{
            //GPS Track recording type set
            startAutomaticGPSTrackRecordingProcedures();
        }


    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();

        stopAutomaticOBDTrackRecordingProcedures();
        stopAutomaticGPSTrackRecordingProcedures();

        this.bus.unregister(this);

        // unregister all boradcast receivers.
        unregisterReceiver(mBroadcastReciever);

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    private void startAutomaticOBDTrackRecordingProcedures(){

        //if bluetooth is not enabled, then don't start the procedures
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) return;

        // Set the Notification
        if (OBDRecordingService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED
                && this.mBluetoothHandler.isBluetoothEnabled()) {
            // State: No OBD device selected.
            if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_OBD_SELECTED);
            } else if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
            } else {
                NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);
            }
        }

        // only start the discovery process if the required settings has been selected.
        if (mBluetoothHandler.isBluetoothEnabled() &&
                mBluetoothHandler.getSelectedBluetoothDevice() != null &&
                mCarManager.getCar() != null &&
                mIsAutoconnect) {
            scheduleDiscovery(-1);
        }
    }

    private void stopAutomaticOBDTrackRecordingProcedures(){
        // Unsubscribe subscriptions.
        if (mWorkerSubscription != null)
            mWorkerSubscription.unsubscribe();
        if (mDiscoverySubscription != null)
            mDiscoverySubscription.unsubscribe();

        // Close the corresponding notification.
        NotificationHandler.closeNotification();
        mBluetoothHandler.stopBluetoothDeviceDiscovery();

    }

    private void startAutomaticGPSTrackRecordingProcedures(){

        //if GPS is turned OFF, then don't start the procedures
        if(!mLocationHandler.isGPSEnabled()) return;

        // Set the Notification
        if (GPSOnlyRecordingService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED) {
            if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
            } else {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NOT_STARTED);
            }
        }

        setGPSAutoConnect();
    }

    private void stopAutomaticGPSTrackRecordingProcedures(){
        // Close the corresponding notification.
        NotificationHandler.closeNotification();

        //remove the transitions
        removeActivityTransitions();

    }

    private void setGPSAutoConnect(){
        if(hasCarSelected && mIsAutoconnect)  setupActivityTransitions();
        else removeActivityTransitions();
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));
        if(recordingTypeSelected == 1){
            if(!event.isBluetoothEnabled) {
                // When Bluetooth has been turned off, then this remoteService is required to be closed.
                if (mBluetoothHandler.isDiscovering())
                    mBluetoothHandler.stopBluetoothDeviceDiscovery();
                stopAutomaticOBDTrackRecordingProcedures();
            }else{
                startAutomaticOBDTrackRecordingProcedures();
            }
        }
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            if(recordingTypeSelected == 2){
                if(!event.mIsGPSEnabled){
                    stopAutomaticGPSTrackRecordingProcedures();
                }else{
                    startAutomaticGPSTrackRecordingProcedures();
                }
            }
        });
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));
        if(recordingTypeSelected == 1){
            if (event.mDevice == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_OBD_SELECTED);
            } else if (NotificationHandler.getRecordingState() == ServiceStateForNotification.NO_OBD_SELECTED) {
                if (mCarManager.getCar() == null) {
                    NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
                } else {
                    NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);
                }
            }
            scheduleDiscovery(-1);
        }
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
                //NotificationHandler.setRecordingState(ServiceStateForNotification.CONNECTING);
                NotificationHandler.closeNotification();
                break;
            case SERVICE_STARTED:
                // NotificationHandler.setRecordingState(ServiceStateForNotification.CONNECTED);
                NotificationHandler.closeNotification();
                if (mWorkerSubscription != null)
                    mWorkerSubscription.unsubscribe();
                break;
            case SERVICE_STOPPING:
                NotificationHandler.setRecordingState(ServiceStateForNotification.STOPPING);
                break;
            case SERVICE_STOPPED:
                if(recordingTypeSelected == 1){
                    NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);
                    scheduleDiscovery(REDISCOVERY_INTERVAL);
                }else{
                    NotificationHandler.setRecordingState(ServiceStateForNotification.NOT_STARTED);
                }
                break;
            case SERVICE_DEVICE_DISCOVERY_RUNNING:
                NotificationHandler.setRecordingState(ServiceStateForNotification.DISCOVERING);
                break;
            case SERVICE_DEVICE_DISCOVERY_PENDING:
                break;
        }
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOGGER.info(String.format("onReceiveNewCarTypeSelectedEvent(): %s", event.toString()));
        if(recordingTypeSelected == 1){
            if(OBDRecordingService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED){
                if (event.mCar == null) {
                    updateNotificationState(ServiceStateForNotification.NO_CAR_SELECTED);
                } else if (OBDRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState
                        .SERVICE_STOPPED) {
                    updateNotificationState(ServiceStateForNotification.UNCONNECTED);
                }
            }
        }else{
            if(GPSOnlyRecordingService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED) {
                if (!hasCarSelected) {
                    NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
                }else{
                    NotificationHandler.setRecordingState(ServiceStateForNotification.NOT_STARTED);
                }
            }
        }

    }

    private void updateNotificationState(ServiceStateForNotification state) {
        if (OBDRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STOPPED) {
            if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_OBD_SELECTED);
            } else if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
            } else {
                ServiceStateForNotification currentState = NotificationHandler.getRecordingState();
                if (currentState != ServiceStateForNotification.DISCOVERING &&
                        state != ServiceStateForNotification.DISCOVERING) {
                    if (mIsAutoconnect) {
                        scheduleDiscovery(REDISCOVERY_INTERVAL);
                    }
                }
                NotificationHandler.setRecordingState(state);
            }
        }
    }


    /**
     * Starts the discovery for the selected OBDII device. If the device has been found then the
     * device either auto-connects or updates the notification accordinlgy depending on the
     * individual settings.
     */
    private void startDiscoveryForSelectedDevice() {
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device == null) {
            mMainThreadWorker.schedule(() -> Toast.makeText(getApplicationContext(), "No paired " +
                    "bluetooth device selected", Toast.LENGTH_SHORT).show());
        } else {
            // If the remoteService is already discovering, then skip the current discovery and
            // unsubscribe on the corresponding subscription.
            if (mDiscoverySubscription != null) {
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
                mDiscoverySubscription.unsubscribe();
                mDiscoverySubscription = null;
            }

            // Initialize a new discovery of the bluetooth.
            mDiscoverySubscription = mBluetoothHandler
                    .startBluetoothDiscoveryForSingleDevice(device)
                    .subscribe(new SafeSubscriber<BluetoothDevice>(new Subscriber<BluetoothDevice>() {

                        private boolean isFound = false;

                        @Override
                        public void onStart() {
                            LOGGER.info("Device Discovery started...");
                            NotificationHandler.setRecordingState(ServiceStateForNotification.DISCOVERING);
                        }

                        @Override
                        public void onNext(BluetoothDevice device) {
                            LOGGER.info("Device Discovered...");

                            // The device has been successfully discovered. Set the flag to true
                            // and stop the discovery process.
                            isFound = true;
                            mBluetoothHandler.stopBluetoothDeviceDiscovery();

                            LOGGER.info("Try to start the connection to " +
                                    "the selected OBD adapter.");

                            getApplicationContext().startService(
                                    new Intent(getApplicationContext(), OBDRecordingService
                                            .class));

                        }

                        @Override
                        public void onCompleted() {
                            LOGGER.info("Device Discovery finished...");

                            // If the device to search for has not been found during the
                            // discovery period, then set back the notification state to
                            // unconnected.
                            if (!isFound) {
                                LOGGER.info("The selected OBDII device has not been found. " +
                                        "Schedule a new discovery in " + mDiscoveryInterval + " " +
                                        "seconds.");
                                NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);

                                // Reschedule the discovery if it is enabled.
                                if (mIsAutoconnect) {
                                    scheduleDiscovery(mDiscoveryInterval);
                                }
                            }

                            if (mDiscoverySubscription != null) {
                                mDiscoverySubscription.unsubscribe();
                                mDiscoverySubscription = null;
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOGGER.error("Error while discovering for the selected Bluetooth " +
                                    "devices", e);
                        }
                    }) {
                        @Override
                        public void onStart() {
                            getActual().onStart();
                        }
                    });
        }
    }

    /**
     * Schedules the immediate discovery for the selected OBDII adapter.
     */
    private void scheduleDiscovery() {
        this.scheduleDiscovery(-1);
    }

    /**
     * Schedules the discovery for the selected OBDII adapter with a specific delay.
     *
     * @param delay time to wait before the scheduled action gets executes. A non-positive delay
     *              indicate an undelayed execution.
     */
    private void scheduleDiscovery(int delay) {
        // Unschedule all outstanding work.
        unscheduleDiscovery();

        // if autoconnect has been enabled and a car has been selected, then schedule a new
        // discovery.
        if (mIsAutoconnect && hasCarSelected && this.mBluetoothHandler.isBluetoothEnabled()) {
            // Reschedule a fresh discovery.
            mWorkerSubscription = mWorkerThread.schedule(() -> {
                startDiscoveryForSelectedDevice();
            }, delay, TimeUnit.SECONDS);

            LOGGER.info("Discovery subscription has been scheduled -> [%s]", "" +
                    delay);
        }
    }

    /**
     * Stops the current discovery and/or the scheduled upcoming discovery.
     */
    private void unscheduleDiscovery() {
        if (mWorkerSubscription != null) {
            if (mBluetoothHandler.isDiscovering())
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
            mWorkerSubscription.unsubscribe();
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

    /**
     * Starts the GPSOnlyConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startGPSOnlyConnectionService() {
        if (!ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDRecordingService.class)) {

            // Start the GPS Only Connection Service
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), OBDRecordingService.class));

        }
    }

    /**
     * Starts the OBDConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startOBDConnectionService() {
        if (!ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDRecordingService.class)) {

            // Start the OBD Connection Service
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), OBDRecordingService.class));

        }
    }


}
