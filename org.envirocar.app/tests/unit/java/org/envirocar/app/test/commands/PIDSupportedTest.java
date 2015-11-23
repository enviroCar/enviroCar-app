/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.test.commands;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import junit.framework.Assert;

import org.envirocar.obd.commands.CommonCommand.CommonCommandState;
import org.envirocar.obd.commands.PIDUtil.PID;
import org.junit.Test;

public class PIDSupportedTest {

	@Test
	public void testPIDSupportedParsing() {
		PIDSupported cmd = new PIDSupported();
		
		cmd.setRawData(createResponseMockup());
		cmd.parseRawData();
		
		Set<PID> result = cmd.getSupportedPIDs();
		
		assertResult(result);
	}

    @Test
	public void testPIDSupportedFail() {
		PIDSupported cmd = new PIDSupported();
		
		cmd.setRawData(createResponseFailMockup());
		cmd.parseRawData();
		
		Assert.assertTrue(cmd.getCommandState() == CommonCommandState.EXECUTION_ERROR);
	}

	private void assertResult(Set<PID> result) {
		Set<PID> expected = new HashSet<PID>();
		expected.add(PID.CALCULATED_ENGINE_LOAD);
		expected.add(PID.FUEL_PRESSURE);
		expected.add(PID.INTAKE_AIR_TEMP);
		expected.add(PID.INTAKE_MAP);
		expected.add(PID.MAF);
		expected.add(PID.RPM);
		expected.add(PID.SPEED);
		
		Assert.assertTrue(String.format(Locale.US, "Size is different. Expected %d, Received %d.",
				expected.size(),
				result.size()),
				result.size() == expected.size());
		
		for (PID string : expected) {
			Assert.assertTrue(result.contains(string));
		}
	}

	private byte[] createResponseMockup() {
		StringBuilder sb = new StringBuilder();
		sb.append("4100");
		sb.append("107B0000");
		return sb.toString().getBytes();
	}

	private byte[] createResponseFailMockup() {
		StringBuilder sb = new StringBuilder();
		sb.append("4100");
		sb.append("107B0000");
		sb.append("4100");
		sb.append("107B0000");
		return sb.toString().getBytes();
	}
	
}

