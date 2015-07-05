package org.envirocar.app.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.preferences.PreferencesConstants;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class SystemStartupService extends Service {
    private static final Logger LOGGER = Logger.getLogger(SystemStartupService.class);

    private static final int REDISCOVERY_INTERVAL = 30;

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";


    // Injected variables
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected NotificationHandler mNotificationHandler;


    private NotificationHandler.NotificationState mCurrentState;
    private Scheduler.Worker mWorkerThread = Schedulers.newThread().createWorker();
    private boolean mIsAutoconnct;
    private int mDiscoveryInterval;


    // private member fields.
    private Subscription mWorkerSubscription;
    private Subscription mDiscoverySubscription;
    private Subscription mSharedPrefSubscription;


    // Background service for the connection to the OBD adapter.
    private OBDConnectionService mOBDConnectionService;
    private boolean mIsOBDConnectionBounded;
    private ServiceConnection mOBDConnectionServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // successfully bounded to the service, cast the binder interface to
            // get the service.
            OBDConnectionService.OBDConnectionBinder binder = (OBDConnectionService
                    .OBDConnectionBinder) service;
            mOBDConnectionService = binder.getService();
            mIsOBDConnectionBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Service has been disconnected.
            mOBDConnectionService = null;
            mIsOBDConnectionBounded = false;
        }
    };
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

                // UNUSED: This leads sometimes to some errors if you always ski
                if (mDiscoverySubscription != null) {
                    mDiscoverySubscription.unsubscribe();
                    mDiscoverySubscription = null;

                    // Set the notification state to unconnected.
                    mNotificationHandler.setNotificationState(SystemStartupService.this,
                            NotificationHandler.NotificationState.UNCONNECTED);
                }
            }

            // Received action matches the command for starting the recording of a track.
            else if (ACTION_START_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Start Track Recording.");
                startOBDConnectionService();
//                mNotificationHandler.setNotificationState(SystemStartupService.this,
//                        NotificationHandler.NotificationState.OBD_FOUND);
            }

            // Received action matches the command for stopping the recording process of a track.
            else if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Stop Track Recording.");

            }
        }
    };

    @Override
    public void onCreate() {
        LOGGER.info("onCreate()");
        super.onCreate();

        // Inject ourselves.
        ((Injector) getApplicationContext()).injectObjects(this);

        // Register on the event bus.
        this.mBus.register(this);

        // Get the required preference settings.
        final SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        this.mIsAutoconnct = preferences.getBoolean(PreferencesConstants
                .PREFERENCE_TAG_BLUETOOTH_AUTOCONNECT, PreferencesConstants
                .DEFAULT_BLUETOOTH_AUTOCONNECT);
        this.mDiscoveryInterval = preferences.getInt(PreferencesConstants
                        .PREFERENCE_TAG_BLUETOOTH_DISCOVERY_INTERVAL,
                PreferencesConstants.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);

        // Set the Notification to
        this.mNotificationHandler.setNotificationState(this,
                NotificationHandler.NotificationState.UNCONNECTED);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_START_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_STOP_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_START_TRACK_RECORDING);
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, notificationClickedFilter);

        // if the OBDConnectionService is running, then bind the service.
        bindOBDConnectionService();

        mSharedPrefSubscription = ContentObservable.fromSharedPreferencesChanges(preferences)
                .filter(prefKey ->
                        PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_AUTOCONNECT.equals(prefKey) ||
                                PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_DISCOVERY_INTERVAL
                                        .equals(prefKey))
                .subscribe(prefKey -> {
                    LOGGER.info(String.format("Received change in preferences [%s]", prefKey));

                    if (prefKey.equals(PreferencesConstants
                            .PREFERENCE_TAG_BLUETOOTH_AUTOCONNECT)) {
                        mIsAutoconnct = preferences.getBoolean(PreferencesConstants
                                .PREFERENCE_TAG_BLUETOOTH_AUTOCONNECT, false);
                    } else {
                        mDiscoveryInterval = preferences.getInt(PreferencesConstants
                                        .PREFERENCE_TAG_BLUETOOTH_DISCOVERY_INTERVAL,
                                PreferencesConstants.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);
                    }
                });
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");

        //
        if (mBluetoothHandler.isBluetoothEnabled()) {
            scheduleDiscovery(-1);
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();

        // Unbind the connection service.
        unbindOBDConnectionService();

        // unregister all boradcast receivers.
        unregisterReceiver(mBroadcastReciever);

        // Unsubscribe subscriptions.
        mSharedPrefSubscription.unsubscribe();
        mWorkerSubscription.unsubscribe();
        if (mDiscoverySubscription != null)
            mDiscoverySubscription.unsubscribe();

        // Close the corresponding notification.
        mNotificationHandler.closeNotification(this);
        mBluetoothHandler.stopBluetoothDeviceDiscovery();
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));

        if (!event.isBluetoothEnabled) {
            // When Bluetooth has been turned off, then this service is required to be closed.
            stopSelf();
        }
    }

    /**
     * Schedules the discovery for the selected OBDII adapter with a specific delay.
     *
     * @param delay time to wait before the scheduled action gets executes. A non-positive delay
     *              indicate an undelayed execution.
     */
    private void scheduleDiscovery(int delay) {
        // Unschedule all outstanding work.
        if (mWorkerSubscription != null)
            mWorkerSubscription.unsubscribe();

        // Reschedule a fresh discovery.
        mWorkerSubscription = mWorkerThread.schedule(() -> {
            startDiscoveryForSelectedDevice();
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Establishes a binding to the OBDConnectionService if the service is running.
     */
    private void bindOBDConnectionService() {
        if (ServiceUtils.isServiceRunning(getApplicationContext(),
                // Defines callbacks for the service binding, passed to bindService()
                OBDConnectionService.class)) {
            // Bind to OBDConnectionService
            Intent intent = new Intent(this, OBDConnectionService.class);
            bindService(intent, mOBDConnectionServiceCon,
                    Context.BIND_ABOVE_CLIENT);
        }
    }

    /**
     * Starts the OBDConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startOBDConnectionService() {
        if (mOBDConnectionService == null && !ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {

            // Start the OBD Connection Service
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), OBDConnectionService.class));

            // binds the OBD Connection Service.
            bindOBDConnectionService();
        }
    }

    /**
     * Removes a binding to the OBDConnection service if the service is running and this service
     * is bound.
     */
    private void unbindOBDConnectionService() {
        // Only when the service is running and this service is bounded to that service.
        if (mOBDConnectionService != null && ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {

            // Unbinds the OBD connection service.
            unbindService(mOBDConnectionServiceCon);
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
//            mWorkerThread.schedule(() -> {
//                Toast.makeText(getApplicationContext(), "No paired bluetooth device " +
//                        "selected", Toast.LENGTH_SHORT).show();
//            });

        } else {
            // If the service is already discovering, then skip the current discovery and
            // unsubscribe on the corresponding subscription.
            if (mDiscoverySubscription != null) {
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
                mDiscoverySubscription.unsubscribe();
                mDiscoverySubscription = null;
            }

            // Initialize a new discovery of the bluetooth.
            mDiscoverySubscription = mBluetoothHandler
                    .startBluetoothDiscoveryForSingleDevice(device)
                    .subscribe(new Subscriber<BluetoothDevice>() {

                        private boolean isFound = false;

                        @Override
                        public void onStart() {
                            LOGGER.info("Device Discovery started...");
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.DISCOVERING);
                        }

                        @Override
                        public void onNext(BluetoothDevice device) {
                            LOGGER.info("Device Discovered...");

                            // The device has been successfully discovered. Set the flag to true
                            // and stop the discovery process.
                            isFound = true;
                            mBluetoothHandler.stopBluetoothDeviceDiscovery();

                            // Depending on the individual settings either start the background
                            // service or update the notification state.
                            if (mIsAutoconnct) {
                                LOGGER.info("[Autoconnect is on]. Try to start the connection to " +
                                        "the selected OBD adapter.");

                                getApplicationContext().startService(
                                        new Intent(getApplicationContext(), OBDConnectionService
                                                .class));
                            } else {
                                LOGGER.info("[Autoconnect is off]. Update the notification.");

                                // If the device has been successful discovered, set the
                                // notification state to OBD_FOUND and stop the bluetooth discovery.
                                mNotificationHandler.setNotificationState(SystemStartupService.this,
                                        NotificationHandler.NotificationState.OBD_FOUND);
                                scheduleDiscovery(REDISCOVERY_INTERVAL);
                            }
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
                                mNotificationHandler.setNotificationState(SystemStartupService.this,
                                        NotificationHandler.NotificationState.UNCONNECTED);

                                scheduleDiscovery(mDiscoveryInterval);
                            } else {

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
                    });
        }
    }
}
