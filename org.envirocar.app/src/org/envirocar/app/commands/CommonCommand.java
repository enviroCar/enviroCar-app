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
		NEW, RUNNING, FINISHED, EXECUTION_ERROR, QUEUE_ERROR
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException,
			InterruptedException {
		sendCommand(out);
		readResult(in);
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
	protected void sendCommand(OutputStream outputStream) throws IOException,
			InterruptedException {
		// add the carriage return char
		command += "\r";

		// write to OutputStream, or in this case a BluetoothSocket
		outputStream.write(command.getBytes());
		outputStream.flush();

		Thread.sleep(100);
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
	protected void readResult(InputStream inputStream) throws IOException {
		byte b = 0;
		StringBuilder stringbuilder = new StringBuilder();

		// read until '>' arrives
		while ((char) (b = (byte) inputStream.read()) != '>')
			if ((char) b != ' ')
				stringbuilder.append((char) b);

		rawData = stringbuilder.toString().trim();
		logger.info("Command name: " + getCommandName() + ", Send '" + getCommand() + "', get raw data '" + rawData + "'");
		
		// clear buffer
		buffer.clear();

		// read string each two chars
		int begin = 0;
		int end = 2;
		while (end <= rawData.length()) {
			String temp = "0x" + rawData.substring(begin, end);
			buffer.add(Integer.decode(temp));
			begin = end;
			end += 2;
		}
	}

	/**
	 * @return the raw command response in string representation.
	 * 
	 * TODO rawData is null, when car switch. What should be done in this case?
	 */
	public String getRawData() {
		if (rawData == null || rawData.contains("SEARCHING") || rawData.contains("DATA")
				//TODO check if cars do this!!
				|| rawData.contains("OK")
				) {
			rawData = "NODATA";
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
		sb.append("Commandname: " + getCommandName());
		sb.append(", Command: " + getCommand());
		sb.append(", RawData: " + getRawData());
		sb.append(", Result: " + getResult());
		return super.toString();
	}

}