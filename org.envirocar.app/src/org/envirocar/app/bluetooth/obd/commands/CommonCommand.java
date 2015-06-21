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

package org.envirocar.app.bluetooth.obd.commands;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract command class that the other commands have to extend. Many things
 * are imported from Android OBD Reader project!
 * 
 * @author jakob
 * 
 */
public abstract class CommonCommand {

	private static Set<Character> ignoredChars;
	private static final char COMMAND_SEND_END = '\r';
	private static final char COMMAND_RECEIVE_END = '>';
	private static final char COMMAND_RECEIVE_SPACE = ' ';
	
	static {
		ignoredChars = new HashSet<Character>();
		ignoredChars.add(COMMAND_RECEIVE_SPACE);
		ignoredChars.add(COMMAND_SEND_END);
	}
	
	private byte[] rawData = null;
	private String command = null;
	private Long commandId;
	private CommonCommandState commandState;
	private String responseTypeId;
	private long resultTime;
	


	/**
	 * Default constructor to use
	 * 
	 * @param command
	 *            the command to send. This will be the raw data send to the OBD device
	 *            (if a sub-class does not override {@link #getOutgoingBytes()}).
	 */
	public CommonCommand(String command) {
		this.command = command;
		determineResponseByte();
		setCommandState(CommonCommandState.NEW);
	}

	private void determineResponseByte() {
		if (this.command == null || this.command.isEmpty()) return;
		
		String[] array = this.command.split(" ");
		if (array != null && array.length > 1) {
			this.responseTypeId = array[1];
		}
	}

	/**
	 * The state of the command.
	 */
	public enum CommonCommandState {
		NEW, RUNNING, FINISHED, EXECUTION_ERROR, QUEUE_ERROR, SEARCHING, UNMATCHED_RESULT
	}


	public String getResponseTypeID() {
		return responseTypeId;
	}


	public boolean responseAlwaysRequired() {
		return true;
	}

	public abstract void parseRawData();


	/**
	 * @return the OBD command name.
	 */
	public abstract String getCommandName();

	/**
	 * @return the commandId
	 */
	public Long getCommandId() {
		return commandId;
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
		sb.append(command);
		sb.append(", Result Time: ");
		sb.append(getResultTime());
		return sb.toString();
	}


	public void setResultTime(long currentTimeMillis) {
		this.resultTime = currentTimeMillis;
	}

	public long getResultTime() {
		return resultTime;
	}

	public byte[] getOutgoingBytes() {
		return command.getBytes();
	}

	public void setRawData(byte[] rawData) {
		this.rawData = rawData;
	}

	public char getEndOfLineReceive() {
		return COMMAND_RECEIVE_END;
	}

	public char getIgnoreCharReceive() {
		return COMMAND_RECEIVE_SPACE;
	}

	public char getEndOfLineSend() {
		return COMMAND_SEND_END;
	}

	public boolean awaitsResults() {
		return true;
	}

	public byte[] getRawData() {
		return this.rawData;
	}

	public Set<Character> getIgnoredChars() {
		return ignoredChars;
	}

}