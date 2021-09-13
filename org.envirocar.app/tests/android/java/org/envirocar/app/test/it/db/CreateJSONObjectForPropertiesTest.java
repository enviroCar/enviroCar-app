/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.test.it.db;
//
//import org.envirocar.app.storage.DbAdapterImpl;
//import org.envirocar.app.storage.Measurement;
//import org.envirocar.app.storage.Measurement.PropertyKey;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import android.support.test.InstrumentationRegistry;
//import android.support.test.runner.AndroidJUnit4;
//import android.test.InstrumentationTestCase;
//
//@RunWith(AndroidJUnit4.class)
//public class CreateJSONObjectForPropertiesTest {
//
//	private DbAdapterImpl dbAdapter;
//
//	@Before
//	protected void setUp() throws Exception {
//
//		this.dbAdapter = new DbAdapterImpl(InstrumentationRegistry.getContext());
//	}
//
//	@Test
//	public void testJsonCreation() throws JSONException {
//		Measurement m = new Measurement(2.0, 3.0);
//		m.setProperty(PropertyKey.CONSUMPTION, 35.5);
//		JSONObject result = this.dbAdapter.createJsonObjectForProperties(m);
//
//		Assert.assertEquals(result.getDouble(PropertyKey.CONSUMPTION.name()), 35.5, 0.0001);
//	}
//
//}
