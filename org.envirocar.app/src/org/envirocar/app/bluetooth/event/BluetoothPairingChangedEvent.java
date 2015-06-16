package org.envirocar.app.bluetooth.event;

import android.bluetooth.BluetoothDevice;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class BluetoothPairingChangedEvent {

    public final BluetoothDevice mBluetoothDevice;
    public final boolean mIsPaired;

    /**
     * Constructor.
     *
     * @param device   the device to which the pairing has been changed.
     * @param isPaired is paired or lost the pairing to.
     */
    public BluetoothPairingChangedEvent(final BluetoothDevice device, final boolean isPaired) {
        this.mBluetoothDevice = device;
        this.mIsPaired = isPaired;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Bluetooth Name", mBluetoothDevice.getName())
                .add("Bluetooth Address", mBluetoothDevice.getAddress())
                .add("isPaired", mIsPaired)
                .toString();
    }
}
