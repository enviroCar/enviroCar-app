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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.app.util.VersionRange;
import org.envirocar.app.util.VersionRange.Version;

import android.test.AndroidTestCase;

public class VersionRangeTest extends AndroidTestCase {
	
	public void testVersionSorting() {
		
		List<Version> list = new ArrayList<Version>();
		list.add(Version.fromString("1.1.0"));
		list.add(Version.fromString("1.0.0"));
		list.add(Version.fromString("5000.0.0"));
		list.add(Version.fromString("1.0.11"));
		list.add(Version.fromString("0.1.0"));
		list.add(Version.fromString("0.1.1-SNAPSHOT"));
		list.add(Version.fromString("0.2.3-SNAPSHOT"));
		list.add(Version.fromString("0.2.0"));
		
		Collections.sort(list);
		
		Assert.assertTrue(list.get(1).equals(Version.fromString("0.1.1-SNAPSHOT")));
		Assert.assertTrue(list.get(2).equals(Version.fromString("0.2.0")));
		Assert.assertTrue(list.get(3).equals(Version.fromString("0.2.3-SNAPSHOT")));
		Assert.assertTrue(list.get(4).equals(Version.fromString("1.0.0")));
		Assert.assertTrue(list.get(5).equals(Version.fromString("1.0.11")));
		Assert.assertTrue(list.get(6).equals(Version.fromString("1.1.0")));
		Assert.assertTrue(list.get(7).equals(Version.fromString("5000.0.0")));
		
	}
	
	public void testVersionParsing() {
		Version v = Version.fromString("23.42.1111-SNAPSHOT");
		
		Assert.assertEquals(23, v.getMajor());
		Assert.assertEquals(42, v.getMinor());
		Assert.assertEquals(1111, v.getFix());
		Assert.assertEquals(true, v.isSnapshot());
	}
	
	public void testRangeParsing() {
		VersionRange range = VersionRange.fromString("(0, 0.8.0]");
		
		Assert.assertEquals(Version.fromString("0.8.0"),range.getMaximum());
		Assert.assertEquals(Version.fromString("0.0.0"),range.getMinimum());
		Assert.assertEquals(true, range.isMaximumIncluded());
		Assert.assertEquals(false, range.isMinimumIncluded());
	}
	
	public void testInRange() {
		VersionRange range = VersionRange.fromString("[0.2, 12.3.2)");
		
		Assert.assertTrue(!range.isInRange(Version.fromString("0.1.9999999")));
		Assert.assertTrue(range.isInRange(Version.fromString("0.2.0")));
		Assert.assertTrue(range.isInRange(Version.fromString("0.2.1")));
		Assert.assertTrue(range.isInRange(Version.fromString("12.3.1")));
		Assert.assertTrue(!range.isInRange(Version.fromString("12.3.2")));
	}

}
