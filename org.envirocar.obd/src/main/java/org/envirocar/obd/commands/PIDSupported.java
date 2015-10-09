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

package org.envirocar.obd.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.envirocar.obd.commands.PIDUtil.PID;


/**
 * Turns off line-feed.
 */
public class PIDSupported extends CommonCommand {

	private Set<PID> pids;
	private byte[] bytes;
	private String group;

	public PIDSupported() {
		this("00");
	}

	/**
	 * @param group the group of commands ("00", "20", "40" ...)
	 */
	public PIDSupported(String group) {
		super("01 ".concat(group));
		this.group = group;
	}

	@Override
	public String getCommandName() {
		return "PID Supported; Group ".concat(group); 
	}


	/**
	 * @return the set of PIDs that are supported by a car,
	 * encoded as their HEX byte strings
	 */
	public Set<PID> getSupportedPIDs() {
		if (pids == null) {
			pids = new HashSet<PID>();
			
			for (int i = 0; i < bytes.length; i++) {
				int current = bytes[i];
				
				for (int bit = 3; bit >= 0; bit--) {
					boolean is = ((current >> bit) & 1 ) == 1;
					if (is) {
						/*
						 * we are starting at PID 01 and not 00
						 */
						PID pid = PIDUtil.fromString(createHex(i*4 + (3-bit) + 1));
						if (pid != null) {
							pids.add(pid);
						}
					}
				}
				
			}
		}
		
		return pids;
	}


	private String createHex(int i) {
		String result = Integer.toString(i, 16);
		if (result.length() == 1) result = "0".concat(result);
		return result;
	}


	@Override
	public void parseRawData() {
		int index = 0;
		int length = 2;

		preprocessRawData();
		
		byte[] data = getRawData();
		
		bytes = new byte[data.length-4];
		
		if (bytes.length != 8) {
			setCommandState(CommonCommandState.EXECUTION_ERROR);
			return;
		}
		
		while (index < data.length) {
			if (index == 0) {
				String tmp = new String(data, index, length);
				// this is the status
				if (!tmp.equals(NumberResultCommand.STATUS_OK)) {
					setCommandState(CommonCommandState.EXECUTION_ERROR);
					return;
				}
				index += length;
				continue;
			}
			else if (index == 2) {
				String tmp = new String(data, index, length);
				// this is the ID byte
				if (!tmp.equals(this.getResponseTypeID())) {
					setCommandState(CommonCommandState.UNMATCHED_RESULT);
					return;
				}
				index += length;
				continue;
			}
			
			/*
			 * this is a hex number
			 */
			bytes[index-4] = (byte) Integer.valueOf(String.valueOf((char) data[index]), 16).intValue();
			if (bytes[index-4] < 0){
				setCommandState(CommonCommandState.EXECUTION_ERROR);
				return;
			}
			index++;
		}
		
		setCommandState(CommonCommandState.FINISHED);
	}


	private void preprocessRawData() {
		byte[] data = getRawData();
		String str = new String(data);
		if (str.contains("4100")) {
			int index = str.indexOf("4100");
			setRawData(Arrays.copyOfRange(data, index, data.length));
		}
	}

}