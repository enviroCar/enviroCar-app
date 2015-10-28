package org.envirocar.core.events.bluetooth;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class BluetoothDiscoveryStateChangedEvent {

    public enum BluetoothDiscoveryState{
        BLUETOOTH_DISCOVERY_PENDING,
        BLUETOOTH_DISCOVERY_STOPPED
    }

    public final BluetoothDiscoveryState mBluetoothDiscoveryState;

    /**
     * Constructor.
     *
     * @param state
     */
    public BluetoothDiscoveryStateChangedEvent(final BluetoothDiscoveryState state){
        this.mBluetoothDiscoveryState = state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("BluetoothDiscoveryState", mBluetoothDiscoveryState)
                .toString();
    }
}
