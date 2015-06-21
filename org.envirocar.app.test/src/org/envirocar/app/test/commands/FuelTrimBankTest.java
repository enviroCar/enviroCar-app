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

import org.envirocar.app.bluetooth.obd.commands.LongTermTrimBank1;
import org.envirocar.app.bluetooth.obd.commands.ShortTermTrimBank1;

import android.test.AndroidTestCase;

public class FuelTrimBankTest extends AndroidTestCase {
	
	public void testShortTermParsing() {
		ShortTermTrimBank1 st = new ShortTermTrimBank1();
		st.setRawData(createShortTermData());
		
		st.parseRawData();
		
		Number result = st.getNumberResult();
		
		Assert.assertTrue("Expected 50.0", result.doubleValue() == 50.0);
	}

	private byte[] createShortTermData() {
		return "4106C0".getBytes();
	}

	public void testLongTermParsing() {
		LongTermTrimBank1 lt = new LongTermTrimBank1();
		lt.setRawData(createLongTermData());
		
		lt.parseRawData();
		
		Number result = lt.getNumberResult();
		
		Assert.assertTrue("Expected -75.0", result.doubleValue() == -75.0);
	}

	private byte[] createLongTermData() {
		return "410720".getBytes();
	}
}
