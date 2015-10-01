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

import org.envirocar.app.model.dao.CacheDirectoryProvider;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.model.dao.InternetAccessProvider;
import org.envirocar.app.util.Util;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

@RunWith(AndroidJUnit4.class)
public class CacheDAOTest {

	private MockupCacheDirectoryProvider mockupDir;


	protected void prepareCache(File baseFolder, String asset, String assetFile) throws IOException {
		InputStream is = InstrumentationRegistry.getContext().getAssets().open(asset);
		FileWriter fw = new FileWriter(new File(baseFolder, assetFile), false);
		
		InputStreamReader isr = new InputStreamReader(is);
		
		while (isr.ready()) {
			fw.write(isr.read());
		}
		
		fw.flush();
		fw.close();
		isr.close();
	}
	
	protected void clearCache(File baseFolder, String assetFile) throws IOException {
		File f = new File(baseFolder, assetFile);
		
		if (f != null && f.exists()) {
			f.delete();
		}
	}

	public DAOProvider getDAOProvider() throws IOException {
		MockupCacheDirectoryProvider mockupDir = getMockupDir();
		DAOProvider prov = new DAOProvider(InstrumentationRegistry.getContext());
		
		return prov;
	}

	protected MockupCacheDirectoryProvider getMockupDir() {
		return mockupDir;
	}

	public static class OfflineProvider implements InternetAccessProvider {

		@Override
		public boolean isConnected() {
			return false;
		}
		
	}
	
	public class MockupCacheDirectoryProvider implements CacheDirectoryProvider {

		private File base;

		public MockupCacheDirectoryProvider() {
			File root = Util.resolveCacheFolder(InstrumentationRegistry.getContext());
			base = new File(root, "test");
			base.mkdir();
		}
		
		@Override
		public File getBaseFolder() {
			return base;
		}
		
	}
	
}