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
package org.envirocar.app.dao.cache;

import java.io.IOException;
import java.util.List;

import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.SensorRetrievalException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.json.JSONException;
import org.json.JSONObject;

public class CacheSensorDAO extends AbstractCacheDAO implements SensorDAO {
	
	private static final Logger logger = Logger.getLogger(CacheSensorDAO.class);
	public static final String CAR_CACHE_FILE_NAME = "cache_cars";
	


	@Override
	public List<Car> getAllSensors() throws SensorRetrievalException {
		try {
			List<Car> result = null;
			
			int c = 1;
			while (true) {
				if (cacheFileExists(CAR_CACHE_FILE_NAME+c)) {
					if (result == null) {
						result = Car.fromJsonList(readCache(CAR_CACHE_FILE_NAME+c));
					}
					else {
						result.addAll(Car.fromJsonList(readCache(CAR_CACHE_FILE_NAME+c)));
					}
				}
				else {
					break;
				}
				c++;
			}
			
			/*
			 * fallback for old cache states
			 */
			if (result == null) {
				result = Car.fromJsonList(readCache(CAR_CACHE_FILE_NAME));
			}
			
			return SensorDAOUtil.sortByManufacturer(result);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		}
	}

	@Override
	public String saveSensor(Car car) throws NotConnectedException {
		throw new NotConnectedException("CacheSensorDAO does not support saving.");
	}

	public void storeAllSensors(List<JSONObject> parentObject) throws IOException {
		int c = 1;
		for (JSONObject jsonObject : parentObject) {
			storeCache(CAR_CACHE_FILE_NAME+(c++), jsonObject.toString());
		}
	}


}
