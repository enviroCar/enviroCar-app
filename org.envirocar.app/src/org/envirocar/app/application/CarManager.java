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
package org.envirocar.app.application;

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.model.Car;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class CarManager {
	
	public static final String PREF_KEY_CAR_MODEL = "carmodel";
	public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
	public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
	public static final String PREF_KEY_FUEL_TYPE = "fueltype";
	public static final String PREF_KEY_SENSOR_ID = "sensorid";
	public static final String PREF_KEY_CAR_ENGINE_DISPLACEMENT = "pref_engine_displacement";
	public static final String CAR_CACHE_FILE_NAME = "cars";
	
	private static CarManager instance = null;
	
	private SharedPreferences preferences;
	private Car car;

	private CarManager(SharedPreferences prefs) {
		this.preferences = prefs;
		
		car = CarSelectionPreference.instantiateCar(preferences.getString(SettingsActivity.CAR, null));
		
		this.preferences.registerOnSharedPreferenceChangeListener(
				new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				if (key.equals(SettingsActivity.CAR)) {
					car = CarSelectionPreference.instantiateCar(preferences.getString(SettingsActivity.CAR, null));
				}
			}
		});
	}
	
	public static synchronized CarManager instance() {
		if (instance == null) {
			// Initialize first
		}
		return instance;
	}
	
	public static void init (SharedPreferences preferences) {
		if (instance == null) {
			instance = new CarManager(preferences);
		}
	}
	
	public Car getCar() {
		return car;
	}
	
	public void setCar(Car c) {
		this.car = c;
	}

}
