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

import org.envirocar.app.logging.Logger;

public abstract class NumberResultCommand extends CommonCommand {

	private static final Logger logger = Logger.getLogger(NumberResultCommand.class);
	
	public NumberResultCommand(String command) {
		super(command);
	}

	@Override
	public void parseRawData() {
		
		int index = 0;
		int length = 2;
		while (index + length <= rawData.length()) {
			try {
				String tmp = rawData.substring(index, index + length);
				if (index == 2) {
					// this is the ID byte
					if (!tmp.equals(this.getResponseByte())) {
						setCommandState(CommonCommandState.UNMATCHED_RESULT);
						return;
					}
				}
				buffer.add(Integer.parseInt(tmp, 16));
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage());
				setCommandState(CommonCommandState.EXECUTION_ERROR);
			}
			index += length;
		}
	}
	
}
