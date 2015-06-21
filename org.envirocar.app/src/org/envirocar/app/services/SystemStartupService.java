package org.envirocar.app.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.bluetooth.service.BluetoothAutoDiscoveryService;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;

import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class SystemStartupService extends Service {
    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    private static final Logger LOGGER = Logger.getLogger(SystemStartupService.class);
    // Runnables
    private final long DISCOVERY_PERIOD = 1000 * 60 * 2;
    // Injected variables
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected NotificationHandler mNotificationHandler;
    // Non-injected variables
    protected BluetoothAutoDiscoveryService mAutoDiscoveryService;
    // Executor service with ListenableFuture return
    private ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors
            .newFixedThreadPool(4));
    private Handler mDiscoveryHandler = new Handler();
    private Runnable mBluetoothDiscoveryRunnable;

    // Broadcast receiver that handles the different actions that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_START_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Start Discovery.");

                // If the bluetooth is currently disabled, then do not issue the discovery.
                if (!mBluetoothHandler.isBluetoothEnabled())
                    LOGGER.severe("Bluetooth is disabled. No Bluetooth discovery is issued");

                startDiscoveryForSelectedDevice();

            } else if (ACTION_STOP_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Stop Discovery.");
                if (mBluetoothDiscoveryRunnable != null) {
                    mBluetoothHandler.stopBluetoothDeviceDiscovery();
                    mNotificationHandler.setNotificationState(SystemStartupService.this,
                            NotificationHandler.NotificationState.UNCONNECTED);
                }
            } else if (ACTION_START_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Start Track Recording.");
                mNotificationHandler.setNotificationState(SystemStartupService.this,
                        NotificationHandler.NotificationState.OBD_FOUND);
            } else if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
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

//            // TODO set preferences.
//            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences
//                    (getApplicationContext());
//            // get auto-connect
//            // get delay.
//
        Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();


        mNotificationHandler.setNotificationState(this,
                NotificationHandler.NotificationState.UNCONNECTED);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_START_BT_DISCOVERY);
        intentFilter.addAction(ACTION_STOP_BT_DISCOVERY);
        intentFilter.addAction(ACTION_START_TRACK_RECORDING);
        intentFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");

        if (mBluetoothHandler.isBluetoothEnabled()) {
            startDiscoveryForSelectedDevice();
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        if (event.isBluetoothEnabled) {
            Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_LONG).show();
            if (mBluetoothDiscoveryRunnable == null) {
                startDiscoveryForSelectedDevice();
                // TODO update Notification
            }
        } else {
            mBluetoothDiscoveryRunnable = null;
        }
    }

    private void onDeviceDiscovered() {

    }

    private void autoConnect() {

    }

    private void startDiscoveryForSelectedDevice() {
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device == null) {
            Toast.makeText(getApplicationContext(), "No paired bluetooth device " +
                    "selected", Toast.LENGTH_SHORT).show();
        } else {
            // Initialize a new Runnable for the discovery of the bluetooth.
            mBluetoothDiscoveryRunnable = new SearchForSingleDeviceRunnable(
                    mBluetoothHandler.getSelectedBluetoothDevice());
            mDiscoveryHandler.post(mBluetoothDiscoveryRunnable);
        }
    }


    /**
     * Runnable that handles the discovery of other devices.
     */
    private final class SearchForSingleDeviceRunnable implements Runnable {

        // the device to discover
        private final BluetoothDevice mDeviceToDiscover;

        /**
         * Constructor.
         *
         * @param deviceToDiscover The Bluetooth device to discover.
         */
        protected SearchForSingleDeviceRunnable(BluetoothDevice deviceToDiscover) {
            Preconditions.checkNotNull(deviceToDiscover, "Input device cannot be null.");
            this.mDeviceToDiscover = deviceToDiscover;
        }

        @Override
        public void run() {
            LOGGER.info("BluetoothDiscoveryRunnable.run()");

            // Issue a new discovery for a single device that only returns a successfull discovered
            // device if it matches the input device.
            mBluetoothHandler.startDiscoveryForSingleDevice(mDeviceToDiscover,
                    new BluetoothHandler.BluetoothDeviceDiscoveryCallback() {

                        private boolean isFound = false;

                        @Override
                        public void onActionDeviceDiscoveryStarted() {
                            LOGGER.info("Device Discovery started...");
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.DISCOVERING);
                        }

                        @Override
                        public void onActionDeviceDiscoveryFinished() {
                            LOGGER.info("Device Discovery finished...");

                            // If the device to search for has not been found during the
                            // discovery period, then set back the notification state to
                            // unconnected.
                            if(!isFound){
                                mNotificationHandler.setNotificationState(SystemStartupService.this,
                                        NotificationHandler.NotificationState.UNCONNECTED);
                            }
                        }

                        @Override
                        public void onActionDeviceDiscovered(BluetoothDevice device) {
                            LOGGER.info(String.format("Device discovered... [name = %s, address = %s]",
                                    device.getName(), device.getAddress()));

                            // If the device has been successfull discovered, set the
                            // notification state to OBD_FOUND and stop the bluetooth discovery.
                            if (device.getAddress().equals(mDeviceToDiscover.getAddress())) {
                                mNotificationHandler.setNotificationState(SystemStartupService.this,
                                        NotificationHandler.NotificationState.OBD_FOUND);
                                isFound = true;
                                mBluetoothHandler.stopBluetoothDeviceDiscovery();

                                // If autoconnect is enabled
                                if(true){

                                }
                            }
                        }
                    });
        }
    }
}
