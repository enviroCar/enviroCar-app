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
import org.envirocar.app.dao.TermsOfUseDAO;
import org.envirocar.app.dao.exception.TermsOfUseRetrievalException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

public class CacheTermsOfUseDAO implements TermsOfUseDAO {

	private static final Logger logger = Logger.getLogger(CacheTermsOfUseDAO.class);
	private static final String LIST_CACHE_FILE_NAME = "tou-list";
	private static final String INSTANCE_CACHE_FILE_NAME = "tou-instance-";
	private CacheDirectoryProvider cacheDirectoryProvider;

	public CacheTermsOfUseDAO(CacheDirectoryProvider cacheDirectoryProvider) {
		this.cacheDirectoryProvider = cacheDirectoryProvider;
	}

	@Override
	public TermsOfUse getTermsOfUse() throws TermsOfUseRetrievalException {
		File directory;
		try {
			directory = cacheDirectoryProvider.getBaseFolder();

			File f = new File(directory, LIST_CACHE_FILE_NAME);

			if (f.isFile()) {
				JSONObject tou = Util.readJsonContents(f);
				return TermsOfUse.fromJson(tou);
			} 
			else {
				throw new TermsOfUseRetrievalException("Local cache file could not be accessed.");
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		}
	}


	@Override
	public TermsOfUseInstance getTermsOfUseInstance(String id) throws TermsOfUseRetrievalException {
		File directory;
		try {
			directory = cacheDirectoryProvider.getBaseFolder();

			File f = new File(directory, INSTANCE_CACHE_FILE_NAME+id);

			if (f.isFile()) {
				JSONObject tou = Util.readJsonContents(f);
				return TermsOfUseInstance.fromJson(tou);
			} 
			else {
				throw new TermsOfUseRetrievalException("Local cache file could not be accessed.");
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		}
	}

	public void storeTermsOfUse(String content) throws IOException {
		File cacheFile = new File(cacheDirectoryProvider.getBaseFolder(), LIST_CACHE_FILE_NAME);
		Util.saveContentsToFile(content, cacheFile);		
	}

	public void storeTermsOfUseInstance(String content, String id) throws IOException {
		File cacheFile = new File(cacheDirectoryProvider.getBaseFolder(), INSTANCE_CACHE_FILE_NAME+id);
		Util.saveContentsToFile(content, cacheFile);				
	}
	

}
