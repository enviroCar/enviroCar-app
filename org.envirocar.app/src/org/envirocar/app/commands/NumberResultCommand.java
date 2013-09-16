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

public abstract class NumberResultCommand extends CommonCommand {

	private int[] buffr;
	
	public NumberResultCommand(String command) {
		super(command);
	}

	@Override
	public void parseRawData() {
		
		int index = 0;
		int length = 2;
		byte[] data = getRawData();
		buffr = new int[data.length / 2];
		while (index + length <= data.length) {
			String tmp = new String(data, index, length);
			if (index == 2) {
				// this is the ID byte
				if (!tmp.equals(this.getResponseTypeID())) {
				setCommandState(CommonCommandState.UNMATCHED_RESULT);
				return;
				}
			}
			
			/*
			 * this is a hex number
			 */
			buffr[index/2] = Integer.parseInt(tmp, 16);
			if (buffr[index/2] < 0){
				setCommandState(CommonCommandState.EXECUTION_ERROR);
				return;
			}
			index += length;
		}
	}
	
	public abstract Number getNumberResult();

	public int[] getBuffer() {
		return buffr;
	}
	
}
