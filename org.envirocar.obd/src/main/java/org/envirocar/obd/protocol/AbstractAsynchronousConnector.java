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
package org.envirocar.obd.protocol;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.adapter.OBDConnector;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.exception.AdapterFailedException;
import org.envirocar.obd.protocol.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAsynchronousConnector implements OBDConnector {

	private static final Logger logger = Logger.getLogger(AbstractAsynchronousConnector.class);
	private InputStream inputStream;
	private OutputStream outputStream;
	private AsynchronousResponseThread responseThread;

	protected abstract List<CommonCommand> getRequestCommands();

	protected abstract List<CommonCommand> getInitializationCommands();

	protected abstract char getRequestEndOfLine();
	
	protected abstract ResponseParser getResponseParser();
	
	protected abstract long getSleepTimeBetweenCommands();
	
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
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
			}
			
			executeCommand(cmd);
		}
	}


	@Override
	public List<CommonCommand> executeRequestCommands() throws IOException,
			AdapterFailedException, ConnectionLostException {
		long sleep = getSleepTimeBetweenCommands();
		for (CommonCommand cmd : getRequestCommands()) {
			executeCommand(cmd);
			
			if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		
		if (responseThread != null) {
			if (responseThread.isRunning()) {
				return responseThread.pullAvailableCommands();
			}
			else {
				throw new ConnectionLostException("ResponseThread has been shutdown");
			}
		}
		return Collections.emptyList();
	}

	private void executeCommand(CommonCommand cmd) throws IOException {
		logger.debug("Sending command: "+cmd.getCommandName());
		
		byte[] bytes = cmd.getOutgoingBytes();
		if (bytes != null && bytes.length > 0) {
			outputStream.write(bytes);
		}
		outputStream.write(getRequestEndOfLine());
		outputStream.flush();		
	}

	@Override
	public void prepareShutdown() {
		if (responseThread != null) {
			responseThread.shutdown();			
		}
	}
	
	@Override
	public void shutdown() {
		if (responseThread != null) {
			try {
				responseThread.join();
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
			}
		}		
	}
	
	protected void startResponseThread() {
		if (responseThread == null || !responseThread.isRunning()) {
			responseThread = new AsynchronousResponseThread(inputStream, getResponseParser());
			responseThread.start();
		}
	}


}
