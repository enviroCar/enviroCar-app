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
import org.envirocar.app.protocol.drivedeck.AsynchronousResponseThread;
import org.envirocar.app.protocol.exception.AdapterFailedException;
import org.envirocar.app.protocol.exception.ConnectionLostException;
import org.envirocar.app.protocol.exception.UnmatchedCommandResponseException;

public abstract class AbstractAsynchronousConnector implements OBDConnector {

	private static final Logger logger = Logger.getLogger(AbstractAsynchronousConnector.class);
	private Object inputMutex;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Object outputMutex;
	private AsynchronousResponseThread responseThread;

	protected abstract List<CommonCommand> getRequestCommands();

	protected abstract List<CommonCommand> getInitializationCommands();

	protected abstract char getRequestEndOfLine();
	
	protected abstract ResponseParser getResponseParser();
	
	@Override
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream, Object socketMutex, Object outputMutex) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.inputMutex = socketMutex;
		this.outputMutex = outputMutex;
		
		startResponseThread();
	}
	
	@Override
	public void executeInitializationCommands() throws IOException,
			AdapterFailedException {
		for (CommonCommand cmd : getInitializationCommands()) {
			executeCommand(cmd);
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
		synchronized (outputMutex) {
			logger.info("Sending CycleCommand: "+new String(cmd.getOutgoingBytes()));
			
			outputStream.write(cmd.getOutgoingBytes());
			outputStream.write(getRequestEndOfLine());
			outputStream.flush();		
		}
	}

	@Override
	public void shutdown() {
		if (responseThread != null) {
			responseThread.setRunning(false);
		}		
	}
	
	private void startResponseThread() {
		if (responseThread == null) {
			responseThread = new AsynchronousResponseThread(inputStream, inputMutex, getResponseParser());
			responseThread.start();
		}
	}


}
