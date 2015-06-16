package org.envirocar.app.bluetooth.event;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class BluetoothStateChangedEvent {

    public final boolean isBluetoothEnabled;

    /**
     * Constructor.
     *
     * @param isBluetoothEnabled    bluetooth state.
     */
    public BluetoothStateChangedEvent(boolean isBluetoothEnabled) {
        this.isBluetoothEnabled = isBluetoothEnabled;
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("isBluetoothEnabled", isBluetoothEnabled)
                .toString();
    }
}
