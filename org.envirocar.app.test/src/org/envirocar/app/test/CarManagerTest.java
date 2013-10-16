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

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

public class CarManagerTest extends AndroidTestCase {

	public CarManagerTest() {
		super();
	}

	public void testCarCreation() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
		CarManager.init(pref);
		Car car = CarManager.instance().getCar();
		
		if (car == null) {
			String carPref = pref.getString(SettingsActivity.CAR, null);
			Car carObject = CarSelectionPreference.instantiateCar(carPref);
			Assert.assertNull("CarManager was not able to retrieve the serialized Car object!",
					carObject);
		} 

		Car c1 = new Car(FuelType.DIESEL, "test", "test", "test", 1234, 1);
		Car c2 = new Car(FuelType.GASOLINE, "test", "test", "test", 1234, 1);
		
		Assert.assertTrue("HashCodes of different car objects were the same!", c1.hashCode() != c2.hashCode());
	}
}
