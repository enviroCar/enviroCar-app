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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.InternetAccessProvider;
import org.envirocar.app.dao.SensorRetrievalException;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.model.Car;
import org.envirocar.app.util.Util;

import android.test.InstrumentationTestCase;

public class SensorDAOTest extends InstrumentationTestCase {
	
	public void testGetAllSensorsCached() throws IOException, SensorRetrievalException {
		MockupCacheDirectoryProvider mockupDir = new MockupCacheDirectoryProvider();
		DAOProvider.init(new OfflineProvider(), mockupDir);
		
		prepareCache(mockupDir.getBaseFolder());
		
		List<Car> sensors = DAOProvider.instance().getSensorDAO().getAllSensors();
		Assert.assertTrue("Expected 1 sensor. Got "+sensors.size(), sensors.size() == 1);
	}
	
	
	private void prepareCache(File baseFolder) throws IOException {
		InputStream is = getInstrumentation().getContext().getAssets().open("sensors_mockup.json");
		FileWriter fw = new FileWriter(new File(baseFolder, CacheSensorDAO.CAR_CACHE_FILE_NAME), false);
		
		InputStreamReader isr = new InputStreamReader(is);
		
		while (isr.ready()) {
			fw.write(isr.read());
		}
		
		fw.flush();
		fw.close();
		isr.close();
	}


	private static class OfflineProvider implements InternetAccessProvider {

		@Override
		public boolean isConnected() {
			return false;
		}
		
	}
	
	private class MockupCacheDirectoryProvider implements CacheDirectoryProvider {

		private File base;

		public MockupCacheDirectoryProvider() throws IOException {
			File root = Util.resolveCacheFolder(getInstrumentation().getTargetContext());
			base = new File(root, "test");
			base.mkdir();
		}
		
		@Override
		public File getBaseFolder() {
			return base;
		}
		
	}

}
