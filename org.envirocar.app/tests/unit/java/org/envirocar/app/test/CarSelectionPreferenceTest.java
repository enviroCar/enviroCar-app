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

import junit.framework.Assert;

import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.junit.Test;

import android.test.AndroidTestCase;

public class CarSelectionPreferenceTest {

	@Test
	public void testCarInstantiation() {
		Car inCar = new Car(FuelType.DIESEL, "h4x0r", "_", "lulz", 1337, 42);
		String result = CarSelectionPreference.serializeCar(inCar);
		Car outCar = CarSelectionPreference.instantiateCar(result);
		
		Assert.assertTrue("input and output cars differ", inCar.equals(outCar));
	}

}
