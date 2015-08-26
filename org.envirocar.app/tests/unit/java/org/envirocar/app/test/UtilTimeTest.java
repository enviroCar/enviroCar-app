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
package org.envirocar.app.test;

import java.text.ParseException;

import junit.framework.Assert;

import org.envirocar.app.util.Util;
import org.junit.Test;

import android.test.AndroidTestCase;

public class UtilTimeTest {

	@Test
	public void testIsoDateToLong() throws ParseException {
		String dateString = "2013-09-25T16:16:44Z";

		long result = Util.isoDateToLong(dateString);
		
		Assert.assertTrue("Unexpected millis value.", result == 1380125804000L);
	}

    @Test
	public void testLongToIsoDate() {
		String stringResult = Util.longToIsoDate(1380125804000L);
		
		Assert.assertTrue("Unexpected formatted date value.", stringResult.equals("2013-09-25T16:16:44Z"));
	}
	
}
