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
package org.envirocar.app.test;

public class FuelingEncoderTest extends ResourceLoadingTestCase {

//
//	@Test
//	public void testTrackJsonCreation() throws JSONException,  IOException, InvalidObjectStateException {
//		String expected = createExpectedJson().toString();
//
//		Fueling f = createFueling();
//
//		String json = new FuelingEncoder().createFuelingJson(f).toString();
//
//		Assert.assertTrue("The JSON was not as expected!", json.equals(expected));
//	}
//
//	@Test
//	public void testExceptionOnIncompleteObject() throws JSONException {
//		Exception exc = null;
//		try {
//			new FuelingEncoder().createFuelingJson(createIncompleteFueling());
//		} catch (InvalidObjectStateException e) {
//			exc = e;
//		}
//
//		Assert.assertNotNull(exc);
//	}
//
//	private Fueling createFueling() {
//        Car car = new Car(FuelType.GASOLINE, "manuf", "modl", "51cac874e4b0a34fb6c5ce94", 1234, 2345);
//		Fueling result = new Fueling();
//		result.setCar(car);
//		result.setComment("test");
//		result.setMissedFuelStop(false);
//		result.setCost(new NumberWithUOM(12.2, "eur"));
//		result.setMileage(new NumberWithUOM(12345, "km"));
//		result.setVolume(new NumberWithUOM(7.2, "l"));
//		result.setTime(new Date(0L));
//		return result;
//	}
//
//
//	private Fueling createIncompleteFueling() {
//		Fueling result = new Fueling();
//		result.setComment("test");
//		result.setMissedFuelStop(false);
//		result.setMileage(new NumberWithUOM(12345, "km"));
//		result.setTime(new Date(0L));
//		return result;
//	}
//
//	private JSONObject createExpectedJson() throws IOException, JSONException {
//		String json = readJsonAsset("/fueling_create_mockup.json");
//		return new JSONObject(json);
//	}

}
