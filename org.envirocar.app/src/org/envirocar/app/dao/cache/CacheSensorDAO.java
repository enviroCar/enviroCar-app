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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.envirocar.app.application.User;
import org.envirocar.app.dao.AbstractSensorDAO;
import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.NotConnectedException;
import org.envirocar.app.dao.SensorRetrievalException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

public class CacheSensorDAO extends AbstractSensorDAO {
	
	private static final Logger logger = Logger.getLogger(CacheSensorDAO.class);
	public static final String CAR_CACHE_FILE_NAME = "cache_cars";
	private CacheDirectoryProvider cacheDirectoryProvider;
	
	public CacheSensorDAO(CacheDirectoryProvider cacheDirectoryProvider) {
		this.cacheDirectoryProvider = cacheDirectoryProvider;
	}

	@Override
	public List<Car> getAllSensors() throws SensorRetrievalException {
		File directory;
		try {
			directory = cacheDirectoryProvider.getBaseFolder();

			File f = new File(directory, CAR_CACHE_FILE_NAME);

			if (f.isFile()) {
				JSONObject cars = Util.readJsonContents(f);
				return createSensorList(cars);
			} 
			else {
				throw new SensorRetrievalException("Local cache file could not be accessed.");
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		}
	}

	public void storeAllSensors(String content) throws IOException {
		File carCacheFile = new File(cacheDirectoryProvider.getBaseFolder(), CAR_CACHE_FILE_NAME);
		Util.saveContentsToFile(content, carCacheFile);		
	}

	@Override
	public String saveSensor(Car car, User user) throws NotConnectedException {
		throw new NotConnectedException("CacheSensorDAO does not support saving.");
	}


}
