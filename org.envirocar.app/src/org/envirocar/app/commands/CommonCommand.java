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
	private String responseByte;
	
	private static final String COMMAND_SEND_END = "\r";
	private static final String NODATA = "NODATA";

	/**
	 * Default constructor to use
	 * 
	 * @param command
	 *            the command to send
	 */
	public CommonCommand(String command) {
		this.command = command;
		determineResponseByte();
		setCommandState(CommonCommandState.NEW);
		this.buffer = new ArrayList<Integer>();
	}

	private void determineResponseByte() {
		String[] array = this.command.split(" ");
		if (array != null && array.length > 1) {
			this.responseByte = array[1];
		}
	}

	/**
	 * The state of the command.
	 */
	public enum CommonCommandState {
		NEW, RUNNING, FINISHED, EXECUTION_ERROR, QUEUE_ERROR, SEARCHING, UNMATCHED_RESULT
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException {

	}
	

	public String getResponseByte() {
		return responseByte;
	}


	public boolean responseAlwaysRequired() {
		return true;
	}

	/**
	 * Override if the sub-command does not get data back from the OBD-II interface
	 * 
	 * @return if the command awaits raw data as a result
	 */
	public boolean awaitsResults() {
		return true;
	}

	public String getEndOfLineSend() {
		return COMMAND_SEND_END;
	}


	public abstract void parseRawData();

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

	public void setRawData(String r) {
		rawData = r;
	}


}