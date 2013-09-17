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
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.exception.AdapterFailedException;
import org.envirocar.app.protocol.exception.ConnectionLostException;
import org.envirocar.app.protocol.exception.UnmatchedCommandResponseException;

public abstract class AbstractAsynchronousConnector implements OBDConnector {

	private static final Logger logger = Logger.getLogger(AbstractAsynchronousConnector.class);
	private InputStream inputStream;
	private OutputStream outputStream;
	private AsynchronousResponseThread responseThread;

	protected abstract List<CommonCommand> getRequestCommands();

	protected abstract List<CommonCommand> getInitializationCommands();

	protected abstract char getRequestEndOfLine();
	
	protected abstract ResponseParser getResponseParser();
	
	@Override
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		
		startResponseThread();
	}
	
	@Override
	public void executeInitializationCommands() throws IOException,
			AdapterFailedException {
		for (CommonCommand cmd : getInitializationCommands()) {
			executeCommand(cmd);
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	@Override
	public List<CommonCommand> executeRequestCommands() throws IOException,
			AdapterFailedException, UnmatchedCommandResponseException,
			ConnectionLostException {
		for (CommonCommand cmd : getRequestCommands()) {
			executeCommand(cmd);
		}
		
		return responseThread.pullAvailableCommands();
	}

	private void executeCommand(CommonCommand cmd) throws IOException {
		logger.debug("Sending command: "+new String(cmd.getOutgoingBytes()));
		
		outputStream.write(cmd.getOutgoingBytes());
		outputStream.write(getRequestEndOfLine());
		outputStream.flush();		
	}

	@Override
	public void shutdown() {
		if (responseThread != null) {
			responseThread.shutdown();
		}		
	}
	
	private void startResponseThread() {
		if (responseThread == null) {
			responseThread = new AsynchronousResponseThread(inputStream, getResponseParser());
			responseThread.start();
		}
	}


}
