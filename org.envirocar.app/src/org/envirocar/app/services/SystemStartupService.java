package org.envirocar.app.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.application.service.BackgroundServiceImpl;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class SystemStartupService extends Service {
    private static final String TAG = SystemStartupService.class.getSimpleName();

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    private static final Logger LOGGER = Logger.getLogger(SystemStartupService.class);
    // Runnables
    private final int DISCOVERY_PERIOD = 1000*5; //1000 * 60 * 2;

    // Injected variables
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected NotificationHandler mNotificationHandler;

    private NotificationHandler.NotificationState mCurrentState;

    private Scheduler.Worker mWorkerThread = Schedulers.newThread().createWorker();


    // private member fields.
    private Subscription mDiscoverySubscription;

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
                if (!mBluetoothHandler.isBluetoothEnabled())
                    LOGGER.severe("Bluetooth is disabled. No Bluetooth discovery is issued");

                startDiscoveryForSelectedDevice();

            }

            // Received action matches the command for stopping the Bluetooth discovery process.
            else if (ACTION_STOP_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Stop Discovery.");

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

        // Set the Notification to
        mNotificationHandler.setNotificationState(this,
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
            scheduleDiscovery();
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        super.onDestroy();

        // Unsubscribe the observable.
        if (mDiscoverySubscription != null && !mDiscoverySubscription.isUnsubscribed()) {
            mDiscoverySubscription.unsubscribe();
        }

        // Unbind the connection service.
        unbindOBDConnectionService();

        // Close the corresponding notification.
        mNotificationHandler.closeNotification(this);
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        if (event.isBluetoothEnabled) {
            Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_LONG).show();
            if (mDiscoverySubscription == null) {
                startDiscoveryForSelectedDevice();
                // TODO update Notification
            }
        } else {
            if (mDiscoverySubscription != null) {
                mDiscoverySubscription.unsubscribe();
                mDiscoverySubscription = null;
            }

            // When Bluetooth has been turned off, then this service is required to close when
            stopSelf();
        }
    }

    /**
     *
     */
    private void scheduleDiscovery(){
        mWorkerThread.schedule(new Action0() {
            @Override
            public void call() {
                startDiscoveryForSelectedDevice();
            }
        });
    }

    /**
     *
     */
    private void scheduleDiscovery(int delay) {
        mWorkerThread.schedule(() -> {
            startDiscoveryForSelectedDevice();
        }, delay, TimeUnit.MILLISECONDS);
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
     * Removes a binding to the OBDConnection service if the service is running and this service
     * is bound.
     */
    private void unbindOBDConnectionService() {
        if (mOBDConnectionService != null && ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {
            unbindService(mOBDConnectionServiceCon);
        }
    }

    /**
     *
     */
    private void startOBDConnectionService() {
        if (mOBDConnectionService == null && !ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), OBDConnectionService.class));
            bindOBDConnectionService();
        }
    }


    private void startDiscoveryForSelectedDevice() {
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device == null) {
            Toast.makeText(getApplicationContext(), "No paired bluetooth device " +
                    "selected", Toast.LENGTH_SHORT).show();
        } else {
            // Initialize a new discovery of the bluetooth.
            mDiscoverySubscription = mBluetoothHandler
                    .startBluetoothDiscoveryForSingleDevice(device)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<BluetoothDevice>() {

                        private boolean isFound = false;

                        @Override
                        public void onStart() {
                            Log.i("YEA", "Device Discovery Started...");
                            LOGGER.info("Device Discovery started...");
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.DISCOVERING);
                        }

                        @Override
                        public void onNext(BluetoothDevice device) {
                            LOGGER.info("Device Discovered...");

                            // If the device has been successful discovered, set the
                            // notification state to OBD_FOUND and stop the bluetooth discovery.
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.OBD_FOUND);
                            isFound = true;
                            mBluetoothHandler.stopBluetoothDeviceDiscovery();

                            // If autoconnect is enabled
                            if (false) {
                                getApplicationContext().startService(
                                        new Intent(getApplicationContext(), BackgroundServiceImpl
                                                .class));
                            }
                        }


                        @Override
                        public void onCompleted() {
                            LOGGER.info("Device Discovery finished...");

                            // If the device to search for has not been found during the
                            // discovery period, then set back the notification state to
                            // unconnected.
                            if (!isFound) {
                                mNotificationHandler.setNotificationState(SystemStartupService.this,
                                        NotificationHandler.NotificationState.UNCONNECTED);
                                scheduleDiscovery(100);
                            } else {

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
