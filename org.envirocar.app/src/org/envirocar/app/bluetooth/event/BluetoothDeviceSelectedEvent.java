package org.envirocar.app.bluetooth.event;

import android.bluetooth.BluetoothDevice;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class BluetoothDeviceSelectedEvent {

    public final BluetoothDevice mDevice;

    /**
     * Constructor.
     *
     * @param device the selected device.
     */
    public BluetoothDeviceSelectedEvent(BluetoothDevice device){
        this.mDevice = device;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Device Name", mDevice.getName())
                .add("Device Address", mDevice.getAddress())
                .toString();
    }
}
