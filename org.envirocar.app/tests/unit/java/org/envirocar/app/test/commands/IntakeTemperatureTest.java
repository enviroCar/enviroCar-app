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

import org.envirocar.obd.commands.CommonCommand.CommonCommandState;
import org.junit.Test;

public class IntakeTemperatureTest {

	@Test
	public void testParsing() {
		byte[] bytes = createBytes();
		IntakeTemperature cmd = new IntakeTemperature();
		cmd.setRawData(bytes);
		cmd.parseRawData();
		
		Assert.assertTrue(cmd.getCommandState() == CommonCommandState.FINISHED);
		
		double temp = cmd.getNumberResult().doubleValue();
		Assert.assertTrue(temp == 23.0);
	}

	private byte[] createBytes() {
		return "410F3F".getBytes();
	}


}
