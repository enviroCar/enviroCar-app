package org.envirocar.app.bluetooth.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class BluetoothAutoDiscoveryService extends Service {
    private static final Logger LOGGER = Logger.getLogger(BluetoothAutoDiscoveryService.class);
    private static final long DISCOVERY_PERIOD = 1000 * 60 * 2;

    // Injected variables.
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    private Handler mHandler;
    private boolean mAutoDiscovering;

    public BluetoothAutoDiscoveryService() {
    }

    @Override
    public void onCreate() {
        LOGGER.info("onCreate()");
        super.onCreate();

        // Inject ourselves
        ((Injector) getApplicationContext()).injectObjects(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");

        mAutoDiscovering = true;

        // Start a new discovery of Bluetooth devices.
        mHandler.post(new BluetoothDiscoveryRunnable());

//        Intent resultIntent = new Intent(this, ListDevicesActivity.class);
//
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(ListDevicesActivity.class);
//        stackBuilder.addNextIntent(resultIntent);
//
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        Notification note = new NotificationCompat.Builder(getApplicationContext()).
//                setSmallIcon(R.drawable.dashboard).
//                setContentTitle("enviroCar").
//                setContentIntent(resultIntent).
//                setContentText(getResources().getText(R.string.device_discovery_pending)).
//                build();
//
//        startForeground(12, note);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     *
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
                }

                @Override
                public void onActionDeviceDiscoveryFinished() {
                    LOGGER.info("Device Discovery finished...");
                }

                @Override
                public void onActionDeviceDiscovered(BluetoothDevice device) {
                    LOGGER.info(String.format("Device discovered... [name = %s, address = %s]",
                            device.getName(), device.getAddress()));

                    BluetoothDevice selectedDevice =
                            mBluetoothHandler.getSelectedBluetoothDevice();

                    // If the discovered device is the selected device, try to autoconnect.
                    if(selectedDevice != null &&
                            selectedDevice.getAddress().equals(device.getAddress())){
                        // TODO start
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast
                                (new Intent(BluetoothConstants.ACTION_BROADCAST));
                    }
                }
            });
        }
    }

//    private class LocalBinder extends Binder implements
}
