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

package org.envirocar.app.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.envirocar.app.logging.Logger;

/**
 * Abstract command class that the other commands have to extend. Many things
 * are imported from Android OBD Reader project!
 * 
 * @author jakob
 * 
 */
public abstract class CommonCommand {

	protected ArrayList<Integer> buffer = null;
	protected String command = null;
	protected String rawData = null;
	private Long commandId;
	private CommonCommandState commandState;
	
	private static final Logger logger = Logger.getLogger(CommonCommand.class);
	private static final long SLEEP_TIME = 25;
	private static final int MAX_SLEEP_TIME = 5000;
	private static final String COMMAND_SEND_END = "\r";
	private static final char COMMAND_RECEIVE_END = '>';
	private static final char COMMAND_RECEIVE_SPACE = ' ';
	private static final String NODATA = "NODATA";
	private static final CharSequence STOPPED = "STOPPED";
	private static final CharSequence SEARCHING = "SEARCHING";

	/**
	 * Default constructor to use
	 * 
	 * @param command
	 *            the command to send
	 */
	public CommonCommand(String command) {
		this.command = command;
		setCommandState(CommonCommandState.NEW);
		this.buffer = new ArrayList<Integer>();
	}

	/**
	 * The state of the command.
	 */
	public enum CommonCommandState {
		NEW, RUNNING, FINISHED, EXECUTION_ERROR, QUEUE_ERROR, SEARCHING
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException {
		sendCommand(out);
		waitForResult(in);
	}

	private void waitForResult(final InputStream in) throws IOException {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
		
		if (!awaitsResults()) return; 
		try {
			int tries = 0;
			while (in.available() <= 0) {
				if (tries++ * SLEEP_TIME > MAX_SLEEP_TIME)
					throw new IOException("OBD-II Request Timeout of "+MAX_SLEEP_TIME +" ms exceeded.");
				
				Thread.sleep(SLEEP_TIME);
			}
			logger.info(getCommandName().concat(Long.toString(System.currentTimeMillis())));
			readResult(in);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}	
	}

	/**
	 * Override if the sub-command does not get data back from the OBD-II interface
	 * 
	 * @return if the command awaits raw data as a result
	 */
	protected boolean awaitsResults() {
		return true;
	}

	/**
	 * Sends the OBD-II request.
	 * 
	 * This method may be overriden in subclasses, such as ObMultiCommand or
	 * TroubleCodesObdCommand.
	 * 
	 * @param command
	 *            The command to send.
	 */
	protected void sendCommand(OutputStream outputStream) throws IOException {
		// write to OutputStream, or in this case a BluetoothSocket
		outputStream.write(command.concat(COMMAND_SEND_END).getBytes());
		outputStream.flush();

	}

	/**
	 * Resends this command.
	 * @deprecated never used!
	 */
	@Deprecated
	protected void resendCommand(OutputStream outputStream) throws IOException,
			InterruptedException {
		outputStream.write("\r".getBytes());
		outputStream.flush();
	}

	/**
	 * Reads the OBD-II response.
	 */
	protected void readResult(InputStream in) throws IOException {
		byte b = 0;
		StringBuilder sb = new StringBuilder();

		// read until '>' arrives
		while ((char) (b = (byte) in.read()) != COMMAND_RECEIVE_END)
			if ((char) b != COMMAND_RECEIVE_SPACE)
				sb.append((char) b);

		rawData = sb.toString().trim();
		
		// clear buffer
		buffer.clear();

		logger.info(getCommandName() +": "+ rawData);

		if (isSearching(rawData)) {
			setCommandState(CommonCommandState.SEARCHING);
			return;
		}
		
		// read string each two chars
		parseRawData();
	}

	private boolean isSearching(String rawData2) {
		return rawData2.contains(SEARCHING) || rawData2.contains(STOPPED);
	}

	protected abstract void parseRawData();

	/**
	 * @return the raw command response in string representation.
	 * 
	 * TODO rawData is null, when car switch. What should be done in this case?
	 */
	public String getRawData() {
		if (rawData == null || rawData.contains("SEARCHING") || rawData.contains("DATA")
//				//TODO check if cars do this!!
//				|| rawData.contains("OK")
				) {
			rawData = NODATA;
		}
		return rawData;
	}

	/**
	 * Returns this command in string representation.
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the OBD command name.
	 */
	public abstract String getCommandName();

	/**
	 * @return a formatted command response in string representation.
	 */
	public abstract String getResult();

	/**
	 * @return the commandId
	 */
	public Long getCommandId() {
		return commandId;
	}

	/**
	 * @param commandId
	 *            the commandId to set
	 */
	public void setCommandId(Long commandId) {
		this.commandId = commandId;
	}

	/**
	 * @return the commandState
	 */
	public CommonCommandState getCommandState() {
		return commandState;
	}

	/**
	 * @param commandState
	 *            the commandState to set
	 */
	public void setCommandState(CommonCommandState commandState) {
		this.commandState = commandState;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Commandname: ");
		sb.append(getCommandName());
		sb.append(", Command: ");
		sb.append(getCommand());
		sb.append(", RawData: ");
		sb.append(getRawData());
		sb.append(", Result: ");
		sb.append(getResult());
		return sb.toString();
	}

	public boolean isNoDataCommand() {
		if (getRawData() != null && (getRawData().equals(NODATA) ||
				getRawData().equals(""))) return true;
		
		if (getResult() != null && (getResult().equals(NODATA) ||
				getResult().equals(""))) return true;
		
		if (getResult() == null || getRawData() == null) return true;
		
		return false;
	}


}