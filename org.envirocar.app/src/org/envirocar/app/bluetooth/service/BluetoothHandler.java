package org.envirocar.app.bluetooth.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.app.Injector;
import org.envirocar.app.bluetooth.service.event.BluetoothDeviceDiscoveredEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class BluetoothHandler {


    private final List<BluetoothConnectionListener> mConnectionListener = new
            ArrayList<BluetoothConnectionListener>();

    // Injected variables.
    @Inject
    protected Context mContext;
    @Inject
    protected Bus mBus;

    private boolean mIsAutoconnecting;

    // The bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    private String mDeviceName;
    private String mDeviceAddress;

    /**
     * Constructor
     *
     * @param context the context of the current scope.
     */
    public BluetoothHandler(Context context) {
        ((Injector) context).injectObjects(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBus.register(this);
    }

    /**
     * Registers the broadcast receiver for discovery related actions and starts
     * the discovery of other devices.
     */
    public void startBluetoothDeviceDiscovery(final BluetoothDeviceDiscoveryCallback callback) {
        Preconditions.checkNotNull(mBluetoothAdapter, "Error BluetoothAdapter has to be " +
                "initialized before");

        // If the device is already discovering, cancel the discovery before starting.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Register for broadcasts when a device is discovered or the discovery has finished.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        // Register a receiver.
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // If the discovery process finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    // Get the BluetoothDevice from the intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // if it is already paired, then do not add it to the array adapter
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        BluetoothDeviceDiscoveredEvent event =
                                new BluetoothDeviceDiscoveredEvent(device);
                        callback.onActionDeviceDiscovered(device);
                    }

                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    callback.onActionDeviceDiscoveryStarted();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    callback.onActionDeviceDiscoveryFinished();
                    mContext.unregisterReceiver(this);
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    // Nothing to do yet
                }
            }
        }, filter);

        mBluetoothAdapter.startDiscovery();
    }

    public void stopBluetoothDeviceDiscovery() {
        // Cancel discovery if it is discovering.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public Set<BluetoothDevice> getPairedBluetoothDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    public boolean isAutoconnecting() {
        return mIsAutoconnecting;
    }

    public void addBluetoothConnectionListener(BluetoothConnectionListener listener) {
        if (!mConnectionListener.contains(listener)) {
            mConnectionListener.add(listener);
        }
    }

    public void removeBluetoothConnectionListener(BluetoothConnectionListener listener) {
        if (mConnectionListener.contains(listener)) {
            mConnectionListener.remove(listener);
        }
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean isBluetoothActive() {
        if (mBluetoothAdapter == null || mBluetoothAdapter.getAddress() == null) {
            return false;
        }
        return true;
    }

    public void startService() {

    }

    public void stopService() {

    }

    /**
     * Initiates the pairing process to a given {@link BluetoothDevice}.
     *
     * @param device   the device to pair to.
     * @param callback the callback listener.
     */
    public void pairDevice(final BluetoothDevice device,
                           final BluetoothDevicePairingCallback callback) {

        // Register a new BroadcastReceiver for BOND_STATE_CHANGED actions.
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // if the action is a change of the pairing state
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                    // Get state and previous state.
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR);
                    final int prevState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDED &&
                            prevState == BluetoothDevice.BOND_BONDING) {
                        // The device has been successfully paired, inform the callback about
                        // the successful pairing.
                        callback.onDevicePaired(device);
                    } else if (state == BluetoothDevice.BOND_NONE && prevState ==
                            BluetoothDevice.BOND_BONDED) {
                        // The device has been successfully unpaired, inform the callback about this
                        Toast.makeText(mContext, "Unpaired", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }, intent);

        // Using reflection to invoke "createBond" method in order to pair with a given device.
        // This method is public in API lvl 18.
        try {
            // Invoke method and get return value
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            boolean value = (boolean) method.invoke(device, (Object[]) null);

            // Check error.
            if (value)
                callback.onPairingStarted(device);
            else
                callback.onPairingError(device);

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void unpairDevice(BluetoothDevice device) {

        // Using reflection to invoke "removeBond" method in order to remove the pairing with
        // a given device. This method is public in API lvl 18.
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            Object value = method.invoke(device, (Object[]) null);

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback interface for the process of pairing with a given device.
     */
    public interface BluetoothDevicePairingCallback {
        /**
         * Called when the pairing process has been started.
         *
         * @param device the device to pair to.
         */
        void onPairingStarted(BluetoothDevice device);

        /**
         * Called when the device has been successfully paired.
         *
         * @param device the successfully paired device.
         */
        void onDevicePaired(BluetoothDevice device);

        /**
         * Called when the start of pairing has thrown an error (e.g., Bluetooth is disabled).
         *
         * @param device the device to which the pairing was intended.
         */
        void onPairingError(BluetoothDevice device);
    }

    /**
     * Callback interface for the bluetooth discovery of other devices.
     */
    public interface BluetoothDeviceDiscoveryCallback {
        /**
         * Called when the discovery has been started.
         */
        void onActionDeviceDiscoveryStarted();

        /**
         * Called when the discovery has been finished.
         */
        void onActionDeviceDiscoveryFinished();

        /**
         * Called when a new unpaired device has been discovered.
         *
         * @param device the newly discovered device that is not already paired.
         */
        void onActionDeviceDiscovered(BluetoothDevice device);
    }

    public interface BluetoothConnectionListener {
        void onDeviceConnected(String deviceName, String deviceAddress);

        void onDeviceDisconnected(String deviceName, String deviceAddress);

        void onConnectionFailure(String deviceName, String deviceAddress);
    }

}
