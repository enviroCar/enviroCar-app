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
package org.envirocar.app.dao.remote;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.dao.SensorRetrievalException;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.network.HTTPClient;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteSensorDAO extends AbstractSensorDAO {
	
	static final Logger logger = Logger.getLogger(RemoteSensorDAO.class);
	private CacheSensorDAO cache;

	public RemoteSensorDAO(CacheSensorDAO cacheSensorDAO) {
		this.cache = cacheSensorDAO;
	}

	@Override
	public List<Car> getAllSensors() throws SensorRetrievalException {
		HttpGet getRequest = new HttpGet(ECApplication.BASE_URL+"/sensors");
		
		getRequest.addHeader("Accept-Encoding", "application/json");
		
		try {
			HttpResponse response = HTTPClient.execute(getRequest);
			
			String content = HTTPClient.readResponse(response.getEntity());
		
			if (cache != null) {
				try {
					cache.storeAllSensors(content);
				}
				catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			
			JSONObject parentObject = new JSONObject(content);
			
			return createSensorList(parentObject);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		}
	}
	
}
