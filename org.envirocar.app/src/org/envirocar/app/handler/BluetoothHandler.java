/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Pair;

import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.events.bluetooth.BluetoothDeviceDiscoveredEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Scheduler;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
@Singleton
public class BluetoothHandler {
    private static final Logger LOGGER = Logger.getLogger(BluetoothHandler.class);


    private final Context context;
    private final Bus bus;

    private final Scheduler.Worker mWorker = Schedulers.io().createWorker();

    private DisposableObserver mDiscoverySubscription;
    private boolean mIsAutoconnecting;

    BroadcastReceiver bluetoothPairingReceiver;

    // The bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter;
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

                        stopBluetoothDeviceDiscovery();
                        if (mDiscoverySubscription != null) {
                            mDiscoverySubscription.dispose();
                            mDiscoverySubscription = null;
                        }

                        break;
                    case BluetoothAdapter.STATE_OFF:
                        LOGGER.debug("Bluetooth State Changed: STATE_OFF");

                        // Post a new event for the changed bluetooth state on the eventbus.
                        BluetoothStateChangedEvent turnedOffEvent =
                                new BluetoothStateChangedEvent(false);
                        bus.post(turnedOffEvent);

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_ON");

                        // Post a new event for the changed bluetooth state on the eventbus.
                        BluetoothStateChangedEvent turnedOnEvent
                                = new BluetoothStateChangedEvent(true, getSelectedBluetoothDevice());
                        bus.post(turnedOnEvent);

                        break;
                    default:
                        LOGGER.debug("Bluetooth State Changed: unknown state");
                        break;
                }
            }
        }
    };


    /**
     * Constructor
     *
     * @param context the context of the current scope.
     */
    @Inject
    public BluetoothHandler(@InjectApplicationScope Context context, Bus bus) {
        this.context = context;
        this.bus = bus;

        // Get the default bluetooth adapter.
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Register ourselves on the eventbus.
        this.bus.register(this);

        // Register this handler class for Bluetooth State Changed broadcasts.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        ContextCompat.registerReceiver(
                this.context, mBluetoothStateChangedReceiver, filter, ContextCompat.RECEIVER_EXPORTED
        );
    }

    @Produce
    public BluetoothStateChangedEvent produceBluetoothStateChangedEvent() {
        return new BluetoothStateChangedEvent(isBluetoothEnabled(), getSelectedBluetoothDevice());
    }

    @Produce
    public BluetoothDeviceSelectedEvent produceBluetoothDeviceSelectedEvent() {
        return new BluetoothDeviceSelectedEvent(getSelectedBluetoothDevice());
    }

    /**
     * Returns the corresponding BluetoothDevice for the attributes stored in the shared
     * preferences.
     *
     * @return The BluetoothDevice for the selected OBDII adapter in the shared preferences.
     */
    public BluetoothDevice getSelectedBluetoothDevice() {
        // No Bluetooth is available. Therefore, return null.
        if (!isBluetoothEnabled())
            return null;

        // Get the preferences of the device.
        Pair<String, String> bluetoothDevice = ApplicationSettings.getSelectedBluetoothAdapterObservable(context).blockingFirst();

        // If the device address is not empty and the device is still a paired device, get the
        // corresponding BluetoothDevice and return it.
        if (!bluetoothDevice.second.equals("")) {
            Set<BluetoothDevice> devices = getPairedBluetoothDevices();
            for (BluetoothDevice device : devices) {
                if (device.getAddress().equals(bluetoothDevice.second))
                    return device;
            }

            // The device is not paired anymore. Therefore, delete everything in the shared
            // preferences related to the preference.
            setSelectedBluetoothDevice(null);
        }
        return null;
    }

    public void setSelectedBluetoothDevice(BluetoothDevice selectedDevice) {
        ApplicationSettings.setSelectedBluetoothAdapter(context, selectedDevice);
        LOGGER.info("Successfully updated shared preferences");
        bus.post(new BluetoothDeviceSelectedEvent(selectedDevice));
    }

    /**
     * Returns the BluetoothDevice of a given address.
     *
     * @param address the address of the required BluetoothDevice.
     * @return the BluetoothDevice of the given address.
     */
    public BluetoothDevice getBluetoothDeviceByAddress(String address) {
        // No Bluetooth is available. Therefore, return null.
        if (!isBluetoothEnabled())
            return null;

        // If the device is still a paired device, get the corresponding BluetoothDevice
        // and return it.
        Set<BluetoothDevice> devices = getPairedBluetoothDevices();
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equals(address))
                return device;
        }
        return null;
    }

    /**
     * Starts the Bluetooth discovery for a specific input device. If the device has been
     * successfull discovered, the callback's onActionDeviceDiscovered is called.
     *
     * @param inputDevice The input device to start a discovery for.
     * @param callback    The callback used to call back information at some convenient time.
     */
    public void startDiscoveryForSingleDevice(final BluetoothDevice inputDevice,
                                              final BluetoothDeviceDiscoveryCallback callback) {
        // First check the input paramters to be not null.
        Preconditions.checkNotNull(inputDevice, "Input device cannot be null");
        Preconditions.checkNotNull(callback, "Input callback cannot be null");

        // Uses the normal discovery routing with a fresh callback that filters the discovered
        // devices based on their address.
        startBluetoothDeviceDiscovery(false, new BluetoothDeviceDiscoveryCallback() {
            @Override
            public void onActionDeviceDiscoveryStarted() {
                // forward callback call
                callback.onActionDeviceDiscoveryStarted();
            }

            @Override
            public void onActionDeviceDiscoveryFinished() {
                // forward callback call
                callback.onActionDeviceDiscoveryFinished();
            }

            @Override
            public void onActionDeviceDiscovered(BluetoothDevice device) {
                // If the address of the input device matches the discovered device, then return
                // the device over the callback.
                if (device.getAddress().equals(inputDevice.getAddress())) {
                    callback.onActionDeviceDiscovered(device);
                }
            }
        });
    }

    /**
     * Returns an Observable that will execute the bluetooth discovery for a specific input
     * device.
     *
     * @param inputDevice the Bluetooth device to search for.
     * @return an observable that searches for a specific device.
     */
    public Observable<BluetoothDevice> startBluetoothDiscoveryForSingleDevice(
            BluetoothDevice inputDevice) {
        return startBluetoothDiscovery()
                .filter(device1 -> inputDevice.getAddress().equals(device1.getAddress()));
    }

    /**
     * Returns an Observable that will execute the search for unpaired devices.
     *
     * @return an observable that executes the search for unpaired devices.
     */
    public Observable<BluetoothDevice> startBluetoothDiscoveryOnlyUnpaired() {
        return startBluetoothDiscovery()
                .filter(device -> device.getBondState() != BluetoothDevice.BOND_BONDED);
    }

    /**
     * @return
     */
    public Observable<BluetoothDevice> startBluetoothDiscovery() {
        return Observable.create((ObservableEmitter<BluetoothDevice> subscriber) -> {
            LOGGER.info("startBluetoothDiscovery(): subscriber call");

            // If the device is already discovering, cancel the discovery before starting.
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();

                // Small timeout such that the broadcast receiver does not receive the first
                // ACTION_DISCOVERY_FINISHED
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mDiscoverySubscription != null) {
                // Cancel the pending subscription.
                mDiscoverySubscription.dispose();
                mDiscoverySubscription = null;
            }

            // Register for broadcasts when a device is discovered or the discovery has finished.
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mDiscoverySubscription = RxBroadcastReceiver.create(context, filter)
                    .subscribeWith(new DisposableObserver<Intent>() {

                        @Override
                        protected void onStart() {
                            super.onStart();
                        }

                        @Override
                        public void onComplete() {
                            LOGGER.info("onCompleted()");
                            subscriber.onComplete();
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOGGER.info("onError()");
                            subscriber.onError(e);
                        }

                        @Override
                        public void onNext(Intent intent) {
                            String action = intent.getAction();
                            LOGGER.info("Discovery: received action = " + action);

                            // If the discovery process has been started.
                            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
//                                subscriber.onStart();
                            }

                            // If the discovery process finds a device
                            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                                // Get the BluetoothDevice from the intent.
                                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice
                                        .EXTRA_DEVICE);
                                // and inform the subscriber.
                                subscriber.onNext(device);
                            }

                            // If the discovery process has been finished.
                            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                                subscriber.onComplete();
                                mWorker.schedule(() -> {
                                    if (!isDisposed()) {
                                        dispose();
                                    }
                                }, 100, TimeUnit.MILLISECONDS);
                            }
                        }
                    });

            subscriber.setDisposable(mDiscoverySubscription);
            mBluetoothAdapter.startDiscovery();
        });
    }


    /**
     * Registers the broadcast receiver for discovery related actions and starts
     * the discovery of other devices. This method filters the paired devices returned over the
     * callback so that no paired device is gonna returned.
     *
     * @param callback the callback instance to get responses over.
     */
    public void startBluetoothDeviceDiscovery(final BluetoothDeviceDiscoveryCallback callback) {
        // check required parameters to be not null
        Preconditions.checkNotNull(callback, "Error: Input callback cannot be null");

        // Call the overloaded method
        startBluetoothDeviceDiscovery(true, callback);
    }


    /**
     * Registers the broadcast receiver for discovery related actions and starts
     * the discovery of other devices.
     *
     * @param filterPairedDevices true, if the callback only returns discovered devices that are
     *                            not paired.
     * @param callback            the callback instance to get responses over.
     */
    public void startBluetoothDeviceDiscovery(final boolean filterPairedDevices,
                                              final BluetoothDeviceDiscoveryCallback callback) {
        Preconditions.checkNotNull(mBluetoothAdapter, "Error BluetoothAdapter has to be " +
                "initialized before");
        Preconditions.checkNotNull(callback, "Error: Input callback cannot be null");

        // If the device is already discovering, cancel the discovery before starting.
        stopBluetoothDeviceDiscovery();

        // Register for broadcasts when a device is discovered or the discovery has finished.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        // Register a receiver.
        ContextCompat.registerReceiver(context, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // If the discovery process finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    // Get the BluetoothDevice from the intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice
                            .EXTRA_DEVICE);

                    // If the device
                    boolean newDevice = (!filterPairedDevices) || (filterPairedDevices && device
                            .getBondState() != BluetoothDevice.BOND_BONDED);
                    if (newDevice) {
                        BluetoothDeviceDiscoveredEvent event =
                                new BluetoothDeviceDiscoveredEvent(device);
                        callback.onActionDeviceDiscovered(device);
                    }

                }
                // If the discovery process has been started.
                else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    callback.onActionDeviceDiscoveryStarted();
                }
                // If the discovery process has been finished.
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    callback.onActionDeviceDiscoveryFinished();
                    BluetoothHandler.this.context.unregisterReceiver(this);
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    // Nothing to do yet
                }
            }
        }, filter, ContextCompat.RECEIVER_EXPORTED);

        mBluetoothAdapter.startDiscovery();
    }


    /**
     * Cancels the disovery of other Bluetooth devices if the bluetooth device is currently in
     * the device discovery process.
     */
    public void stopBluetoothDeviceDiscovery() {
        // Cancel discovery if it is discovering.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            LOGGER.info("Bluetooth discovery cancled");
        }
    }

    /**
     * Return the set of {@link BluetoothDevice} objects that are bonded
     * (paired) to the local adapter.
     *
     * @return the set of already paired Bluetooth devices.
     */
    public Set<BluetoothDevice> getPairedBluetoothDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    public boolean isAutoconnecting() {
        return mIsAutoconnecting;
    }


    public boolean isBluetoothEnabled() {
        if (mBluetoothAdapter != null)
            return mBluetoothAdapter.isEnabled();
        return false;
    }


//    public boolean isBluetoothActive() {
//        return mBluetoothAdapter != null && mBluetoothAdapter.getAddress() != null;
//    }

    public void enableBluetooth(Activity activity) {
        // If Bluetooth is not enabled, request that it will be enabled.
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, -1);
        }
    }

    /**
     * Return true if the local Bluetooth adapter is currently in the device
     * discovery process.
     *
     * @return true if discovering.
     */
    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public void disableBluetooth(Activity activity) {
        // If Bluetooth is enabled, request that it will be enabled.
        if (isBluetoothEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    /**
     * Initiates the pairing process to a given {@link BluetoothDevice}.
     *
     * @param device   the device to pair to.
     * @param callback the callback listener.
     */
    public void pairDevice(final BluetoothDevice device,
                           final BluetoothDevicePairingCallback callback) {
        if (bluetoothPairingReceiver == null) {
            bluetoothPairingReceiver = new BroadcastReceiver() {
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
                        } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice
                                .BOND_BONDING) {
                            // It was not able to successfully establishing a pairing to the given
                            // device. Inform the callback
                            callback.onPairingError(device);
                        }
                    }
                }
            };
            // Register a new BroadcastReceiver for BOND_STATE_CHANGED actions.
            IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            ContextCompat.registerReceiver(context, bluetoothPairingReceiver, intent, ContextCompat.RECEIVER_EXPORTED);
        }

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

    /**
     * Removes the pairing to a given {@link BluetoothDevice}.
     *
     * @param device   the device to which the pairing should be removed.
     * @param callback the callback listener to inform about successes or errors.
     */
    public void unpairDevice(final BluetoothDevice device, final BluetoothDeviceUnpairingCallback
            callback) {

        // Register a new BroadcastReceiver for BOND_STATE_CHANGED actions.
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        ContextCompat.registerReceiver(context, new BroadcastReceiver() {
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

                    if (state == BluetoothDevice.BOND_NONE &&
                            prevState == BluetoothDevice.BOND_BONDED) {
                        // The device has been successfully unpaired, inform the callback about this
                        callback.onDeviceUnpaired(device);
                        BluetoothHandler.this.context.unregisterReceiver(this);
                    } else if (state == BluetoothDevice.ERROR) {
                        callback.onUnpairingError(device);
                        BluetoothHandler.this.context.unregisterReceiver(this);
                    }
                }
            }
        }, intent, ContextCompat.RECEIVER_EXPORTED);

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
     * Callback interface for unpairing with a given device.
     */
    public interface BluetoothDeviceUnpairingCallback {
        /**
         * Called when the device has been successfully unpaired.
         *
         * @param device the successfully unpaired device.
         */
        void onDeviceUnpaired(BluetoothDevice device);

        /**
         * Called when the start of unpairing has thrown an error.
         *
         * @param device the device to unpair.
         */
        void onUnpairingError(BluetoothDevice device);
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

}
