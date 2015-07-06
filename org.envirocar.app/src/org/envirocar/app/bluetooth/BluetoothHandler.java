package org.envirocar.app.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.app.bluetooth.event.BluetoothDeviceDiscoveredEvent;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.services.ServiceUtils;
import org.envirocar.app.view.preferences.PreferencesConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.functions.Action1;

/**
 * @author dewall
 */
public class BluetoothHandler {
    private static final Logger LOGGER = Logger.getLogger(BluetoothHandler.class);

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
                            mDiscoverySubscription.unsubscribe();
                            mDiscoverySubscription = null;
                        }

                        break;
                    case BluetoothAdapter.STATE_OFF:
                        LOGGER.debug("Bluetooth State Changed: STATE_OFF");

                        // Post a new event for the changed bluetooth state on the eventbus.
                        BluetoothStateChangedEvent turnedOffEvent =
                                new BluetoothStateChangedEvent(false);
                        mBus.post(turnedOffEvent);

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        LOGGER.debug("Bluetooth State Changed: STATE_ON");

                        // Post a new event for the changed bluetooth state on the eventbus.
                        BluetoothStateChangedEvent turnedOnEvent
                                = new BluetoothStateChangedEvent(true);
                        mBus.post(turnedOnEvent);

                        break;
                    default:
                        LOGGER.debug("Bluetooth State Changed: unknown state");
                        break;
                }
            }
        }
    };

    // Injected variables.
    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;


    private Subscription mDiscoverySubscription;
    private boolean mIsAutoconnecting;


    // The bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter;


    /**
     * Constructor
     *
     * @param context the context of the current scope.
     */
    public BluetoothHandler(Context context) {
        // Inject ourselves.
        ((Injector) context).injectObjects(this);

        // Get the default bluetooth adapter.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register ourselves on the eventbus.
        mBus.register(this);

        // Register this handler class for Bluetooth State Changed broadcasts.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStateChangedReceiver, filter);
    }

    /**
     * Starts the connection to the bluetooth device if not already active.
     */
    public void startOBDConnectionService() {
        if (!ServiceUtils.isServiceRunning(mContext, OBDConnectionService.class))
            mContext.getApplicationContext().startService(
                    new Intent(mContext, OBDConnectionService.class));
    }


    public void stopOBDConnectionService() {
        if (ServiceUtils.isServiceRunning(mContext, OBDConnectionService.class))
            mContext.getApplicationContext().stopService(new Intent(mContext,
                    OBDConnectionService.class));
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String deviceName = preferences.getString(PreferencesConstants
                .PREFERENCE_TAG_BLUETOOTH_NAME, PreferencesConstants.PREFERENCE_TAG_EMPTY);
        String deviceAddress = preferences.getString(PreferencesConstants
                .PREFERENCE_TAG_BLUETOOTH_ADDRESS, PreferencesConstants.PREFERENCE_TAG_EMPTY);

        // If the device address is not empty and the device is still a paired device, get the
        // corresponding BluetoothDevice and return it.
        if (!deviceAddress.equals(PreferencesConstants.PREFERENCE_TAG_EMPTY)) {
            Set<BluetoothDevice> devices = getPairedBluetoothDevices();
            for (BluetoothDevice device : devices) {
                if (device.getAddress().equals(deviceAddress))
                    return device;
            }

            // The device is not paired anymore. Therefore, delete everything in the shared
            // preferences related to the preference.
            preferences.edit().remove(PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_NAME)
                    .remove(PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_ADDRESS).commit();
        }
        return null;
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
        return Observable.create(subscriber -> {
            LOGGER.info("startBluetoothDiscovery(): subscriber call");
            // If the device is already discovering, cancel the discovery before starting.
            if (mBluetoothAdapter.isDiscovering() && mDiscoverySubscription != null) {
                mBluetoothAdapter.cancelDiscovery();

                // Cancel the pending subscription.
                mDiscoverySubscription.unsubscribe();
                mDiscoverySubscription = null;
            }

            // Register for broadcasts when a device is discovered or the discovery has finished.
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mDiscoverySubscription = ContentObservable.fromBroadcast(mContext, filter)
                    .subscribe(new Action1<Intent>() {
                        @Override
                        public void call(Intent intent) {
                            String action = intent.getAction();
                            LOGGER.info("Discovery: received action = " + action);

                            // If the discovery process has been started.
                            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                                subscriber.onStart();
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
                                subscriber.onCompleted();

                                if (mDiscoverySubscription != null) {
                                    mDiscoverySubscription.unsubscribe();
                                    mDiscoverySubscription = null;
                                }
                            }
                        }
                    });

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
        mContext.registerReceiver(new BroadcastReceiver() {
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
                    mContext.unregisterReceiver(this);
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    // Nothing to do yet
                }
            }
        }, filter);

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

    public boolean isBluetoothActive() {
        if (mBluetoothAdapter == null || mBluetoothAdapter.getAddress() == null) {
            return false;
        }
        return true;
    }

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
                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice
                            .BOND_BONDING) {
                        // It was not able to successfully establishing a pairing to the given
                        // device. Inform the callback
                        callback.onPairingError(device);
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

                    if (state == BluetoothDevice.BOND_NONE &&
                            prevState == BluetoothDevice.BOND_BONDED) {
                        // The device has been successfully unpaired, inform the callback about this
                        callback.onDeviceUnpaired(device);
                        mContext.unregisterReceiver(this);
                    } else if (state == BluetoothDevice.ERROR) {
                        callback.onUnpairingError(device);
                        mContext.unregisterReceiver(this);
                    }
                }
            }
        }, intent);

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

    public void startService() {

    }

    public void stopService() {

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
