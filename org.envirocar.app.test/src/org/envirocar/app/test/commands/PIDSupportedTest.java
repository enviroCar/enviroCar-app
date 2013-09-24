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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import junit.framework.Assert;

import org.envirocar.app.commands.PIDSupported;

import android.test.AndroidTestCase;

public class PIDSupportedTest extends AndroidTestCase {

	public void testPIDSupportedParsing() {
		PIDSupported cmd = new PIDSupported();
		
		cmd.setRawData(createResponseMockup(cmd));
		cmd.parseRawData();
		
		Set<String> result = cmd.getSupportedPIDs();
		
		assertResult(result);
	}

	private void assertResult(Set<String> result) {
		Set<String> expected = new HashSet<String>();
		expected.add("01");
		expected.add("03");
		expected.add("04");
		expected.add("05");
		expected.add("06");
		expected.add("07");
		expected.add("20");
		
		Assert.assertTrue(String.format(Locale.US, "Size is different. Expected %d, Received %d.",
				expected.size(),
				result.size()),
				result.size() == expected.size());
		
		for (String string : expected) {
			Assert.assertTrue(result.contains(string));
		}
	}

	private byte[] createResponseMockup(PIDSupported cmd) {
		StringBuilder sb = new StringBuilder();
		sb.append("41");
		sb.append(cmd.getResponseTypeID());
		sb.append("BE000001");
		return sb.toString().getBytes();
	}
	
}

