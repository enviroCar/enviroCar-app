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
package org.envirocar.app.test.commands;

import junit.framework.Assert;

import org.envirocar.app.commands.FuelSystemStatus;
import org.envirocar.app.commands.PIDUtil;
import org.envirocar.app.commands.PIDUtil.PID;

import android.test.AndroidTestCase;

public class FuelSystemStatusTest extends AndroidTestCase {

	public void testCommandParsing() {
		FuelSystemStatus cmd = (FuelSystemStatus) PIDUtil.instantiateCommand(PID.FUEL_SYSTEM_STATUS.toString());
		
		cmd.setRawData(createRawDataOpenLoop());
		cmd.parseRawData();
		
		Assert.assertTrue("Expected to be in open loop.", !cmd.isInClosedLoop());
		
		cmd = (FuelSystemStatus) PIDUtil.instantiateCommand(PID.FUEL_SYSTEM_STATUS.toString());
		
		cmd.setRawData(createRawDataClosedLoop(cmd));
		cmd.parseRawData();
		
		Assert.assertTrue("Expected to be in closed loop.", cmd.isInClosedLoop());
	}

	private byte[] createRawDataClosedLoop(FuelSystemStatus cmd) {
		return "41030100".getBytes();	}

	private byte[] createRawDataOpenLoop() {
		return "41030200".getBytes();
	}
	
}
