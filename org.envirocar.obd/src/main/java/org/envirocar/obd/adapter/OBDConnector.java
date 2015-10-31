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
package org.envirocar.obd.adapter;

import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.protocol.exception.AdapterFailedException;
import org.envirocar.obd.protocol.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import rx.Observable;

/**
 * Interface for a OBD connector. It can provide device specific
 * command requests and initialization sequences.
 * 
 * @author matthes rieke
 *
 */
public interface OBDConnector {

	
	enum ConnectionState {
		
		/**
		 * used to indicate a state when the connector could
		 * not understand any response received
		 */
		DISCONNECTED,
		
		/**
		 * used to indicate a state when the connector understood
		 * at least one command. Return this state only if the
		 * adapter is sure, that it can interact with the device
		 * - but the device yet did not return measurements
		 */
		CONNECTED,

		/**
		 * used to indicate a state where the connector received
		 * a parseable measurement
		 */
		VERIFIED
	}

	Observable<Boolean> initialize(InputStream is, OutputStream os);

	Observable<DataResponse> observe();
	
	/**
	 * An implementation shall return true if it 
	 * might support the given bluetooth device.
	 * 
	 * @param deviceName the bluetooth device name
	 * @return if it suggests support for the device
	 */
	boolean supportsDevice(String deviceName);

}
