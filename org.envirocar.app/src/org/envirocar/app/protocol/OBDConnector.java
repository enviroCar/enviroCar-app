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
package org.envirocar.app.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;

/**
 * Interface for a OBD connector. It can provide device specific
 * command requests and initialization sequences.
 * 
 * @author matthes rieke
 *
 */
public interface OBDConnector {

	/**
	 * provide the required stream objects to send and retrieve
	 * commands.
	 * 
	 * An implementation shall synchronize on the socketMutex
	 * when accessing the streams.
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @param socketMutex
	 */
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream, Object socketMutex);

	/**
	 * An implementation shall return true if it 
	 * might support the given bluetooth device.
	 * 
	 * @param deviceName the bluetooth device name
	 * @return if it suggests support for the device
	 */
	public boolean supportsDevice(String deviceName);

	/**
	 * @return true if the implementation established a meaningful connection
	 */
	public boolean connectionVerified();

	/**
	 * an implementation shall use this method to initialize the connection
	 * to the underlying obd adapter
	 * 
	 * @throws IOException if an exception occurred while accessing the stream objects
	 * @throws AdapterFailedException if the adapter could not establish a connection
	 */
	public void executeInitializationCommands() throws IOException,
			AdapterFailedException;

	/**
	 * an implementation shall execute the commands to retrieve
	 * the common phenomena
	 * 
	 * @return the parsed command responses
	 * @throws IOException if an exception occurred while accessing the stream objects
	 * @throws AdapterFailedException if the adapter could not establish a connection
	 * @throws UnmatchedCommandResponseException if the response did not match the requested command
	 * @throws ConnectionLostException if the maximum number of unmatched responses exceeded
	 */
	public List<CommonCommand> executeRequestCommands() throws IOException,
			AdapterFailedException, UnmatchedCommandResponseException, ConnectionLostException;

}
