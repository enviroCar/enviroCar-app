package org.envirocar.app.services;

import android.app.PendingIntent;
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
import android.util.Log;
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
    protected final BroadcastReceiver mBluetoothStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        LOGGER.debug("Bluetooth State Changed: STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        LOGGER.debug("Bluetooth State Changed: STATE_OFF");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_ON");
                        Toast.makeText(getApplicationContext(), "Bluetooth enabled22", Toast
                                .LENGTH_LONG).show();

                        break;
                    default:
                        LOGGER.debug("Bluetooth State Changed: unknown state");
                        break;
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


        // Register this handler class for Bluetooth State Changed broadcasts.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStateChangedReceiver, filter);

        mNotificationHandler.startNotificationForService(this);
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
            // update notification
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


    /**
     * Runnable that handles the discoery of other devices.
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
                            mNotificationHandler.setNotificationState(
                                    NotificationHandler.NotificationState.DISCOVERING);
                        }

                        @Override
                        public void onActionDeviceDiscoveryFinished() {
                            LOGGER.info("Device Discovery finished...");
                            mNotificationHandler.setNotificationState(NotificationHandler
                                    .NotificationState.UNCONNECTED);
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
                            }
                        }
                    });
        }
    }
}
