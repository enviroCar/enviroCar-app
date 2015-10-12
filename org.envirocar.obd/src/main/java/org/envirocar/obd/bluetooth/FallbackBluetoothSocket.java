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
package org.envirocar.obd.bluetooth;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class FallbackBluetoothSocket extends NativeBluetoothSocket {

	private BluetoothSocket fallbackSocket;

	public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
		super(tmp);
        try
        {
          Class<?> clazz = tmp.getRemoteDevice().getClass();
          Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
          Method m = clazz.getMethod("createRfcommSocket", paramTypes);
          Object[] params = new Object[] {Integer.valueOf(1)};
          fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
        }
        catch (Exception e)
        {
        	throw new FallbackException(e);
        }
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return fallbackSocket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return fallbackSocket.getOutputStream();
	}


	@Override
	public void connect() throws IOException {
		fallbackSocket.connect();
	}


	@Override
	public void close() throws IOException {
		fallbackSocket.close();
	}

	
	public static class FallbackException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FallbackException(Exception e) {
			super(e);
		}
		
	}
}
