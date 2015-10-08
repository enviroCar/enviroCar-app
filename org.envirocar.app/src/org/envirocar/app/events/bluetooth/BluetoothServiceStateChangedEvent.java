package org.envirocar.app.events.bluetooth;

import com.google.common.base.MoreObjects;

import org.envirocar.app.bluetooth.service.BluetoothServiceState;


/**
 * @author dewall
 */
public final class BluetoothServiceStateChangedEvent {

    public final BluetoothServiceState mState;

    /**
     * Constructor.
     *
     * @param state the current state of the app.
     */
    public BluetoothServiceStateChangedEvent(BluetoothServiceState state) {
        this.mState = state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("State", mState.toString())
                .toString();
    }
}
