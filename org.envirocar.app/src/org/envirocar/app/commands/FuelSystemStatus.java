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

import org.envirocar.app.commands.PIDUtil.PID;

public class FuelSystemStatus extends CommonCommand {

	public static final String NAME = "Fuel System Status";
	private int setBit;

	public FuelSystemStatus() {
		super("01 ".concat(PID.FUEL_SYSTEM_STATUS.toString()));
	}

	@Override
	public void parseRawData() {
		/*
		 * big try catch as it is not robustly tested
		 */
		try {
			int index = 0;
			int length = 2;
			byte[] data = getRawData();
			
			
			if (data.length != 6 && data.length != 8) {
				setCommandState(CommonCommandState.EXECUTION_ERROR);
			}
			
			while (index < data.length) {
				String tmp = new String(data, index, length);
				if (index == 0) {
					
					// this is the status
					if (!tmp.equals(NumberResultCommand.STATUS_OK)) {
						setCommandState(CommonCommandState.EXECUTION_ERROR);
						return;
					}
					index += length;
					continue;
				}
				else if (index == 2) {
					// this is the ID byte
					if (!tmp.equals(this.getResponseTypeID())) {
						setCommandState(CommonCommandState.UNMATCHED_RESULT);
						return;
					}
					index += length;
					continue;
				}
				else if (index == 4) {
					int value = Integer.valueOf(tmp, 16);
					setBit = determineSetBit(value);
					if (setBit == -1) {
						setCommandState(CommonCommandState.EXECUTION_ERROR);
						return;
					}
					index += length;
				}
				else if (index == 6) {
					//TODO: Second fuel system. not supported yet
					index += length;
				}
				
			}
		} catch (RuntimeException e) {
			setCommandState(CommonCommandState.EXECUTION_ERROR);
		}
	}

	private int determineSetBit(int value) {
		if (value == 0) {
			return 0;
		}
		else if (value == 1) {
			return 1;
		}
		else if (value == 2) {
			return 2;
		}
		else if (value == 4) {
			return 3;
		}
		else if (value == 8) {
			return 4;
		}
		
		return -1;
	}

	public boolean isInClosedLoop() {
		switch (setBit) {
		case 0:
			//Open loop due to insufficient engine temperature
			return false;
		case 1:
			//Closed loop, using oxygen sensor feedback to determine fuel mix
			return true;
		case 2:
			//Open loop due to engine load OR fuel cut due to deceleration
			return false;
		case 3:
			//Open loop due to system failure
			return false;
		case 4:
			//Closed loop, using at least one oxygen sensor but there is a fault in the feedback system
			return true;
		default:
			return false;
		}
	}

	@Override
	public String getCommandName() {
		return NAME;
	}
	
	public int getStatus() {
		return setBit;
	}

}
