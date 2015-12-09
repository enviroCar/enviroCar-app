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
package org.envirocar.app.test;

import android.os.Environment;
import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class, Logger.class, Base64.class})
public class UtilTimeTest {

//	@Test
//	public void testIsoDateToLong() throws ParseException {
//		String dateString = "2013-09-25T16:16:44Z";
//
//		long result = Util.isoDateToLong(dateString);
//
//		Assert.assertTrue("Unexpected millis value.", result == 1380125804000L);
//	}
//
//    @Test
//	public void testLongToIsoDate() {
//		String stringResult = Util.longToIsoDate(1380125804000L);
//
//		Assert.assertTrue("Unexpected formatted date value.", stringResult.equals("2013-09-25T16:16:44Z"));
//	}
	
}
