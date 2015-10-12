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
package org.envirocar.remote.dao;

import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.logging.Logger;

import java.util.List;

import rx.Observable;

public class CacheTermsOfUseDAO extends AbstractCacheDAO implements TermsOfUseDAO {
	private static final Logger logger = Logger.getLogger(CacheTermsOfUseDAO.class);
	private static final String LIST_CACHE_FILE_NAME = "tou-list";
	private static final String INSTANCE_CACHE_FILE_NAME = "tou-instance-";

	@Override
	public TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException {
		return null;
	}

	@Override
	public Observable<TermsOfUse> getTermsOfUseObservable(String id) {
		return null;
	}

	@Override
	public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException {
		return null;
	}

	@Override
	public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
		return null;
	}


	//	@Override
//	public TermsOfUse getTermsOfUse() throws TermsOfUseRetrievalException {
//		try {
//			return TermsOfUse.fromJson(readCache(LIST_CACHE_FILE_NAME));
//		} catch (IOException e) {
//			logger.warn(e.getMessage());
//			throw new TermsOfUseRetrievalException(e);
//		} catch (JSONException e) {
//			logger.warn(e.getMessage());
//			throw new TermsOfUseRetrievalException(e);
//		}
//	}
//
//	@Override
//	public Observable<TermsOfUse> getTermsOfUseObservable() {
//		return null;
//	}
//
//	@Override
//	public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException {
//		return null;
//	}
//
//	@Override
//	public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
//		return null;
//	}
//
//	@Override
//	public TermsOfUseInstance getTermsOfUseInstance(String id) throws TermsOfUseRetrievalException {
//		try {
//			return TermsOfUseInstance.fromJson(readCache(INSTANCE_CACHE_FILE_NAME+id));
//		} catch (IOException e) {
//			logger.warn(e.getMessage());
//			throw new TermsOfUseRetrievalException(e);
//		} catch (JSONException e) {
//			logger.warn(e.getMessage());
//			throw new TermsOfUseRetrievalException(e);
//		}
//	}
//
//	@Override
//	public Observable<TermsOfUseInstance> getTermsOfUseInstanceObservable(String id) {
//		return null;
//	}
//
//	public void storeTermsOfUse(String content) throws IOException {
//		storeCache(LIST_CACHE_FILE_NAME, content);
//	}
//
//	public void storeTermsOfUseInstance(String content, String id) throws IOException {
//		storeCache(INSTANCE_CACHE_FILE_NAME+id, content);
//	}
	

}