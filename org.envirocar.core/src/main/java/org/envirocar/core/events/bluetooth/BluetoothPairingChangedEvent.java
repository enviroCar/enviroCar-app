/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.events.bluetooth;

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
