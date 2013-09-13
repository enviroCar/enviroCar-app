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

public class FallbackBluetoothSocket extends NativeBluetoothSocket {

	private BluetoothSocket fallbackSocket;

	public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
		super(tmp);
        try
        {
          Class<?> localClass = tmp.getRemoteDevice().getClass();
          Class<?>[] arrayOfClass = new Class[1];
          arrayOfClass[0] = Integer.TYPE;
          Method localMethod = localClass.getMethod("createRfcommSocket", arrayOfClass);
          Object[] arrayOfObject = new Object[1];
          arrayOfObject[0] = Integer.valueOf(1);
          fallbackSocket = (BluetoothSocket) localMethod.invoke(tmp.getRemoteDevice(), arrayOfObject);
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
