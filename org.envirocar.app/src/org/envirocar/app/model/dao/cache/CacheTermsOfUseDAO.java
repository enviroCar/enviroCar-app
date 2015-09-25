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
package org.envirocar.app.model.dao.cache;

import java.io.IOException;

import org.envirocar.app.model.dao.TermsOfUseDAO;
import org.envirocar.app.model.dao.exception.TermsOfUseRetrievalException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.json.JSONException;

import rx.Observable;

public class CacheTermsOfUseDAO extends AbstractCacheDAO implements TermsOfUseDAO {

	private static final Logger logger = Logger.getLogger(CacheTermsOfUseDAO.class);
	private static final String LIST_CACHE_FILE_NAME = "tou-list";
	private static final String INSTANCE_CACHE_FILE_NAME = "tou-instance-";


	@Override
	public TermsOfUse getTermsOfUse() throws TermsOfUseRetrievalException {
		try {
			return TermsOfUse.fromJson(readCache(LIST_CACHE_FILE_NAME));
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		}
	}

	@Override
	public Observable<TermsOfUse> getTermsOfUseObservable() {
		return null;
	}

	@Override
	public TermsOfUseInstance getTermsOfUseInstance(String id) throws TermsOfUseRetrievalException {
		try {
			return TermsOfUseInstance.fromJson(readCache(INSTANCE_CACHE_FILE_NAME+id));
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		}
	}

	@Override
	public Observable<TermsOfUseInstance> getTermsOfUseInstanceObservable(String id) {
		return null;
	}

	public void storeTermsOfUse(String content) throws IOException {
		storeCache(LIST_CACHE_FILE_NAME, content);
	}

	public void storeTermsOfUseInstance(String content, String id) throws IOException {
		storeCache(INSTANCE_CACHE_FILE_NAME+id, content);
	}
	

}
