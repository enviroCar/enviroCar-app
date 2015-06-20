package org.envirocar.app.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.bluetooth.service.BluetoothAutoDiscoveryService;
import org.envirocar.app.bluetooth.service.BluetoothConstants;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;

import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class SystemStartupService extends Service {
    private static final Logger LOGGER = Logger.getLogger(SystemStartupService.class);

    public static final int FLAG_BROADCAST_NOTIFICATION_CLICKS = 9555;
    public static final String FLAG_ACTION_START_DISCOVERY = "action_start_discovery";

    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(FLAG_ACTION_START_DISCOVERY.equals(action)){
                LOGGER.info("Received Broadcast: Start Discovery");
                Toast.makeText(getApplicationContext(), "Started Discovery", Toast.LENGTH_SHORT)
                        .show();
                // TODO
                mBluetoothDiscoveryRunnable = new BluetoothDiscoveryRunnable();
                mDiscoveryHandler.post(mBluetoothDiscoveryRunnable);

                if(mBluetoothDiscoveryRunnable != null){

                }
            }
        }
    };



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


    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");


        if (mBluetoothHandler.isBluetoothEnabled()) {
            mBluetoothDiscoveryRunnable = new BluetoothDiscoveryRunnable();
            mDiscoveryHandler.post(mBluetoothDiscoveryRunnable);
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
                mBluetoothDiscoveryRunnable = new BluetoothDiscoveryRunnable();
                mDiscoveryHandler.post(mBluetoothDiscoveryRunnable);
                // TODO update Notification
            }
        } else {
            mBluetoothDiscoveryRunnable = null;
            // TODO update Notification
        }
    }

    private void onDeviceDiscovered(){
        mNotificationHandler.setNotificationState(this,
                NotificationHandler.NotificationState.OBD_FOUND);
    }


    /**
     * Runnable that handles the discovery of other devices.
     */
    private final class BluetoothDiscoveryRunnable implements Runnable {
        @Override
        public void run() {
            LOGGER.info("BluetoothDiscoveryRunnable.run()");
            mBluetoothHandler.startBluetoothDeviceDiscovery(
                    new BluetoothHandler.BluetoothDeviceDiscoveryCallback() {
                        @Override
                        public void onActionDeviceDiscoveryStarted() {
                            LOGGER.info("Device Discovery started...");
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.DISCOVERING);
                        }

                        @Override
                        public void onActionDeviceDiscoveryFinished() {
                            LOGGER.info("Device Discovery finished...");
                            mNotificationHandler.setNotificationState(SystemStartupService.this,
                                    NotificationHandler.NotificationState.UNCONNECTED);
                        }

                        @Override
                        public void onActionDeviceDiscovered(BluetoothDevice device) {
                            LOGGER.info(String.format("Device discovered... [name = %s, address = %s]",
                                    device.getName(), device.getAddress()));

                            BluetoothDevice selectedDevice =
                                    mBluetoothHandler.getSelectedBluetoothDevice();

                            // If the discovered device is the selected device, try to autoconnect.
                            if (selectedDevice != null &&
                                    selectedDevice.getAddress().equals(device.getAddress())) {
                                // TODO start
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast
                                        (new Intent(BluetoothConstants.ACTION_BROADCAST));
                                onDeviceDiscovered();
                            }
                        }
                    });
        }
    }
}
