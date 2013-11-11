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
package org.envirocar.app.test.dao;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.dao.exception.SensorRetrievalException;
import org.envirocar.app.model.Car;

public class SensorDAOTest extends CacheDAOTest {
	
	public void testGetAllSensorsCached() throws IOException, SensorRetrievalException {
		DAOProvider prov = getDAOProvider();
		
		prepareCache(getMockupDir().getBaseFolder(), "sensors_mockup.json", CacheSensorDAO.CAR_CACHE_FILE_NAME);
		
		SensorDAO dao = prov.getSensorDAO();
		List<Car> sensors = dao.getAllSensors();
		Assert.assertTrue("Expected 1 sensor. Got "+sensors.size(), sensors.size() == 1);
	}
	
}
