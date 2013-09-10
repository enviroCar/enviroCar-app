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
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;

public abstract class AbstractOBDConnector implements OBDConnector {
	
	private static final Logger logger = Logger.getLogger(AbstractOBDConnector.class.getName());
	private static final int SLEEP_TIME = 25;
	private static final int MAX_SLEEP_TIME = 5000;
	private static final char COMMAND_RECEIVE_END = '>';
	private static final char COMMAND_RECEIVE_SPACE = ' ';
	private static final CharSequence SEARCHING = "SEARCHING";
	private static final CharSequence STOPPED = "STOPPED";
	protected InputStream inputStream;
	protected OutputStream outputStream;
	protected Object socketMutex;
	private boolean connectionEstablished;

	@Override
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream, Object socketMutex) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.socketMutex = socketMutex;
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
	
	protected void runCommand(CommonCommand cmd)
			throws IOException {
		synchronized (socketMutex) {
			logger.debug("Sending command " +cmd.getCommandName()+ " / "+ cmd.getCommand());
			sendCommand(cmd);
			waitForResult(cmd);	
		}
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
	protected void sendCommand(CommonCommand cmd) throws IOException {
		// write to OutputStream, or in this case a BluetoothSocket
		outputStream.write(cmd.getCommand().concat(cmd.getEndOfLineSend()).getBytes());
		outputStream.flush();
	}
	
	protected void waitForResult(CommonCommand cmd) throws IOException {
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
			logger.debug(cmd.getCommandName().concat(Long.toString(System.currentTimeMillis())));
			readResult(cmd);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}	
	}
	
	/**
	 * Reads the OBD-II response.
	 * @param cmd 
	 */
	protected void readResult(CommonCommand cmd) throws IOException {
		logger.debug("Reading response...");
		
		String rawData = readResponseLine(inputStream);
		cmd.setRawData(rawData);

		logger.debug(cmd.getCommandName() +": "+ rawData);

		if (isSearching(rawData)) {
			cmd.setCommandState(CommonCommandState.SEARCHING);
			return;
		}
		
		// read string each two chars
		cmd.parseRawData();
	}

	public static String readResponseLine(InputStream in) throws IOException {
		byte b = 0;
		StringBuilder sb = new StringBuilder();

		// read until '>' arrives
		while ((char) (b = (byte) in.read()) != COMMAND_RECEIVE_END)
			if ((char) b != COMMAND_RECEIVE_SPACE)
				sb.append((char) b);

		return sb.toString().trim();
	}


	private boolean isSearching(String rawData2) {
		return rawData2.contains(SEARCHING) || rawData2.contains(STOPPED);
	}

	public int getMaxTimeout() {
		return MAX_SLEEP_TIME;
	}

	public int getSleepTime() {
		return SLEEP_TIME;
	}
	
	protected abstract List<CommonCommand> getInitializationCommands();
	
	@Override
	public abstract boolean supportsDevice(String deviceName);

	protected abstract void processInitializationCommand(CommonCommand cmd);

	@Override
	public abstract boolean connectionVerified();

	@Override
	public void executeInitializationCommands() throws IOException, AdapterFailedException {
		List<CommonCommand> cmds = this.getInitializationCommands();
		
		executeCommands(cmds);		
	}
	
	private void executeCommands(List<CommonCommand> cmds) throws IOException, AdapterFailedException {
		for (CommonCommand c : cmds) {
			executeCommand(c);
		}
	}

	private void executeCommand(CommonCommand cmd) throws AdapterFailedException, IOException {
		try {
			if (cmd.getCommandState().equals(CommonCommandState.NEW)) {

				// Run the job
				synchronized (socketMutex) {
					cmd.setCommandState(CommonCommandState.RUNNING);
					runCommand(cmd);
				}
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
			logger.warn("Error while sending command '" + cmd.toString() + "'", e);
			cmd.setCommandState(CommonCommandState.EXECUTION_ERROR);
		}

		// Finished if no more job is in the waiting-list

		if (cmd != null) {
			if (cmd.getCommandState() == CommonCommandState.EXECUTION_ERROR) {
				logger.warn("Execution Error for" +cmd.getCommandName() +" / "+cmd.getCommand());
				return;
			}
			
			else if (cmd.getCommandState() == CommonCommandState.UNMATCHED_RESULT) {
				logger.warn("Did not receive the expected result! Expected: "+cmd.getResponseByte());
				try {
					logger.info("Trying to read another command.");
					AbstractOBDConnector.readResponseLine(inputStream);
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
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
			
			if (!cmd.awaitsResults()) return;

			if (!connectionEstablished && !cmd.isNoDataCommand()) {
				processInitializationCommand(cmd);
				if (connectionVerified()) {
					connectionEstablished = true;
				}
			}
			else {
				cmd.setCommandState(CommonCommandState.FINISHED);
			}
		}
		
	}

	@Override
	public List<CommonCommand> executeRequestCommands() throws IOException, AdapterFailedException {
		List<CommonCommand> list = getRequestCommands();
		
		for (CommonCommand cmd : list) {
			executeCommand(cmd);
		}
		
		return list;
	}
	
}
