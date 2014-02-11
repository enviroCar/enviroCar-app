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

import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Convenience abstract base CacheDAO class.
 */
public class AbstractCacheDAO {

	private CacheDirectoryProvider cacheDirectoryProvider;

	public AbstractCacheDAO(CacheDirectoryProvider cacheDirectoryProvider) {
		this.cacheDirectoryProvider = cacheDirectoryProvider;
	}

	public JSONObject readCache(String cachedFile) throws IOException, JSONException {
		File directory = cacheDirectoryProvider.getBaseFolder();

		File f = new File(directory, cachedFile);

		if (f.isFile()) {
			JSONObject tou = Util.readJsonContents(f);
			return tou;
		} 
		
		throw new IOException(String.format("Could not read file %s", cachedFile));
	}
	
	protected void storeCache(String cacheFileName, String content) throws IOException {
		File file = new File(this.cacheDirectoryProvider.getBaseFolder(), cacheFileName);
		Util.saveContentsToFile(content, file);		
	}
	
}
