/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothSocket;

public class NativeBluetoothSocket implements BluetoothSocketWrapper {

	private BluetoothSocket socket;

	public NativeBluetoothSocket(BluetoothSocket tmp) {
		this.socket = tmp;
		for (Method m : socket.getClass().getMethods()) {
			System.out.println(m);
		}
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
