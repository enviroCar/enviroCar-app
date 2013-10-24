
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
 */package org.envirocar.app.dao.remote;

import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractSensorDAO implements SensorDAO {
	
	private static final Logger logger = Logger.getLogger(AbstractSensorDAO.class);

	protected List<Car> createSensorList(JSONObject carsObject) throws JSONException {
		JSONArray cars = carsObject.getJSONArray("sensors");
		
		List<Car> sensors = new ArrayList<Car>(cars.length());
		
		for (int i = 0; i < cars.length(); i++){
			String typeString;
			JSONObject properties;
			String carId;
			try {
				typeString = ((JSONObject) cars.get(i)).optString("type", "none");
				properties = ((JSONObject) cars.get(i)).getJSONObject("properties");
				carId = properties.getString("id");
			} catch (JSONException e) {
				logger.warn(e.getMessage(), e);
				continue;
			}
			if (typeString.equals(CarSelectionPreference.SENSOR_TYPE)) {
				try {
					sensors.add(Car.fromJsonWithStrictEngineDisplacement(properties));
				} catch (JSONException e) {
					logger.warn(String.format("Car '%s' not supported: %s", carId != null ? carId : "null", e.getMessage()));
				}
			}	
		}
		
		return sensors;
	}

}