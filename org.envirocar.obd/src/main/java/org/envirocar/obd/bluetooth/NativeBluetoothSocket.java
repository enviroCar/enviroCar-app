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
package org.envirocar.obd.bluetooth;

import android.bluetooth.BluetoothSocket;

import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO JavaDoc
 */
public class NativeBluetoothSocket extends BluetoothSocketWrapper {

	private BluetoothSocket socket;

	public NativeBluetoothSocket(BluetoothSocket tmp) {
		this.socket = tmp;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public String getRemoteDeviceName() {
		return socket.getRemoteDevice().getName();
	}

	@Override
	public void connect() throws IOException {
		socket.connect();
	}

	@Override
	public String getRemoteDeviceAddress() {
		return socket.getRemoteDevice().getAddress();
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public BluetoothSocket getUnderlyingSocket() {
		return socket;
	}

}
