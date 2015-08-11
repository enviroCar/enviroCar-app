package org.envirocar.app.events.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.google.common.base.MoreObjects;


/**
 * Event class that holds all the necessary information for a
 * newly discovered bluetooth device.
 *
 * @author dewall
 */
public class BluetoothDeviceDiscoveredEvent {

    public final BluetoothDevice mBluetoothDevice;

    /**
     * Constructor
     *
     * @param bluetoothDevice the new discovered bluetooth device
     */
    public BluetoothDeviceDiscoveredEvent(final BluetoothDevice bluetoothDevice) {
        this.mBluetoothDevice = bluetoothDevice;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("DeviceName", mBluetoothDevice.getName())
                .add("DeviceAddress", mBluetoothDevice.getAddress())
                .add("isBonded", mBluetoothDevice.getBondState())
                .toString();
    }
}
