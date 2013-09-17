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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.exception.AdapterFailedException;
import org.envirocar.app.protocol.exception.ConnectionLostException;
import org.envirocar.app.protocol.exception.UnmatchedCommandResponseException;

/**
 * This class acts as the basis for adapters which work
 * in a request/response fashion (in particular, they do not
 * send out data without an explicit request)
 * 
 * @author matthes rieke
 * @deprecated sequential processing is not very efficient. use {@link AbstractAsynchronousConnector} instead
 */
@Deprecated
public abstract class AbstractSequentialConnector implements OBDConnector {
	
	private static final Logger logger = Logger.getLogger(AbstractSequentialConnector.class.getName());
	private static final int SLEEP_TIME = 25;
	private static final int MAX_SLEEP_TIME = 5000;
	private static final CharSequence SEARCHING = "SEARCHING";
	private static final CharSequence STOPPED = "STOPPED";
	private static final int MAX_INVALID_RESPONSE_COUNT = 5;
	private InputStream inputStream;
	private OutputStream outputStream;
	private boolean connectionEstablished;
	private boolean staleConnection;
	private int invalidResponseCount;
	
	/**
	 * @return the list of initialization commands for the adapter
	 */
	protected abstract List<CommonCommand> getInitializationCommands();

	/**
	 * a sub-class shall process the given command and determine
	 * the connection state when a specific set of command results
	 * have been rececived.
	 * 
	 * @param cmd the executed command
	 */
	protected abstract void processInitializationCommand(CommonCommand cmd);
	
	@Override
	public abstract boolean supportsDevice(String deviceName);

	@Override
	public abstract boolean connectionVerified();

	@Override
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}
	
	protected List<CommonCommand> getRequestCommands() {
		List<CommonCommand> result = new ArrayList<CommonCommand>();
		result.add(new Speed());
		result.add(new MAF());
		result.add(new RPM());
		result.add(new IntakePressure());
		result.add(new IntakeTemperature());
		return result;
	}
	
	private void runCommand(CommonCommand cmd)
			throws IOException {
		logger.debug("Sending command " +cmd.getCommandName()+ " / "+ new String(cmd.getOutgoingBytes()));
		sendCommand(cmd);
		
		waitForResult(cmd);	
	}
	
	/**
	 * Sends the OBD-II request.
	 * 
	 * This method may be overriden in subclasses, such as ObMultiCommand or
	 * TroubleCodesObdCommand.
	 * 
	 * @param cmd
	 *            The command to send.
	 */
	private void sendCommand(CommonCommand cmd) throws IOException {
		// write to OutputStream, or in this case a BluetoothSocket
		outputStream.write(cmd.getOutgoingBytes());
		outputStream.write(cmd.getEndOfLineSend());
		outputStream.flush();
	}
	
	private void waitForResult(CommonCommand cmd) throws IOException {
//		try {
//			Thread.sleep(SLEEP_TIME);
//		} catch (InterruptedException e) {
//			logger.warn(e.getMessage(), e);
//		}
		
		if (!cmd.awaitsResults()) return; 
		try {
			int tries = 0;
			while (inputStream.available() <= 0) {
				if (tries++ * getSleepTime() > getMaxTimeout()) {
					if (cmd.responseAlwaysRequired()) {
						throw new IOException("OBD-II Request Timeout of "+ getMaxTimeout() +" ms exceeded.");
					}
					else {
						return;
					}
				}
				
				Thread.sleep(getSleepTime());
			}
			
			readResult(cmd);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}	
	}
	
	/**
	 * Reads the OBD-II response.
	 * @param cmd 
	 */
	private void readResult(CommonCommand cmd) throws IOException {
		byte[] rawData = readResponseLine(cmd);
		String str = new String(rawData);
		logger.info(str);
		cmd.setRawData(rawData);
		cmd.setResultTime(System.currentTimeMillis());

		logger.debug(cmd.toString());
		String dataString = new String(rawData);

		if (isSearching(dataString) || isNoDataCommand(dataString)) {
			cmd.setCommandState(CommonCommandState.SEARCHING);
			return;
		}
		
		// read string each two chars
		cmd.parseRawData();
	}

	private byte[] readResponseLine(CommonCommand cmd) throws IOException {
		byte b = 0;

		byte[] buffer = new byte[32];
		int index = 0;
		// read until '>' arrives
		while ((char) (b = (byte) inputStream.read()) != cmd.getEndOfLineReceive()) {
			if ((char) b != cmd.getIgnoreCharReceive()){
				buffer[index++] = b;
			}
		}

		return Arrays.copyOf(buffer, index);
	}


	private boolean isSearching(String dataString) {
		return dataString.contains(SEARCHING) || dataString.contains(STOPPED);
	}
	
	private boolean isNoDataCommand(String dataString) {
		return "NODATA".equals(dataString);
	}

	public int getMaxTimeout() {
		return MAX_SLEEP_TIME;
	}

	public int getSleepTime() {
		return SLEEP_TIME;
	}
	
	@Override
	public void executeInitializationCommands() throws IOException, AdapterFailedException {
		List<CommonCommand> cmds = this.getInitializationCommands();
		
		try {
			executeCommands(cmds);
		} catch (UnmatchedCommandResponseException e) {
			logger.warn("This should never happen!", e);
		} catch (ConnectionLostException e) {
			logger.warn("This should never happen!", e);
		}		
	}
	
	@Override
	public List<CommonCommand> executeRequestCommands() throws IOException, AdapterFailedException, UnmatchedCommandResponseException, ConnectionLostException {
		List<CommonCommand> list = getRequestCommands();
		
		for (CommonCommand cmd : list) {
			executeCommand(cmd);
		}
		
		return list;
	}
	
	/**
	 * Execute a list of commands
	 * 
	 * @throws AdapterFailedException if the adapter could not establish a connection
	 * @throws IOException if an exception occurred while accessing the stream objects
	 * @throws UnmatchedCommandResponseException if the response did not match the requested command
	 * @throws ConnectionLostException if the maximum number of unmatched responses exceeded
	 */
	private void executeCommands(List<CommonCommand> cmds) throws AdapterFailedException, IOException, UnmatchedCommandResponseException, ConnectionLostException {
		for (CommonCommand c : cmds) {
			executeCommand(c);
		}
	}

	/**
	 * Execute one command using the given stream objects
	 * 
	 * @throws AdapterFailedException if the adapter could not establish a connection
	 * @throws IOException if an exception occurred while accessing the stream objects
	 * @throws UnmatchedCommandResponseException if the response did not match the requested command
	 * @throws ConnectionLostException if the maximum number of unmatched responses exceeded
	 */
	private void executeCommand(CommonCommand cmd) throws AdapterFailedException, IOException, UnmatchedCommandResponseException, ConnectionLostException {
		try {
			if (cmd.getCommandState().equals(CommonCommandState.NEW)) {

				// Run the job
				cmd.setCommandState(CommonCommandState.RUNNING);
				runCommand(cmd);
			}
		} catch (IOException e) {
			if (!connectionEstablished) {
				/*
				 * lets first try a different adapter before we fail!
				 */
				throw new AdapterFailedException(getClass().getName());
			} else {
				throw e;
			}
			
			
		} catch (Exception e) {
			logger.warn("Error while sending command '" + cmd.toString() + "': "+e.getMessage(), e);
			cmd.setCommandState(CommonCommandState.EXECUTION_ERROR);
		}

		// Finished if no more job is in the waiting-list

		if (cmd != null) {
			if (!cmd.awaitsResults()) return;
			
			if (cmd.getCommandState() == CommonCommandState.EXECUTION_ERROR) {
				logger.warn("Execution Error for" +cmd.getCommandName() +" / "+new String(cmd.getOutgoingBytes()));
				return;
			}
			
			else if (cmd.getCommandState() == CommonCommandState.UNMATCHED_RESULT) {
				logger.warn("Did not receive the expected result! Expected: "+cmd.getResponseTypeID());
				
				if (staleConnection && invalidResponseCount++ > MAX_INVALID_RESPONSE_COUNT) {
					throw new ConnectionLostException();
				}
				else {
					staleConnection = true;
					throw new UnmatchedCommandResponseException();	
				}
				
			}
			
			else if (cmd.getCommandState() == CommonCommandState.SEARCHING) {
				logger.info("Adapter still searching. Waiting a bit.");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
				}
				return;
			}
			
			if (!connectionEstablished && !isNoDataCommand(new String(cmd.getRawData()))) {
				processInitializationCommand(cmd);
				if (connectionVerified()) {
					connectionEstablished = true;
				}
			}
			else {
				cmd.setCommandState(CommonCommandState.FINISHED);
				if (staleConnection) {
					staleConnection = false;
					invalidResponseCount = 0;
				}
			}
		}
		
	}

	
}
