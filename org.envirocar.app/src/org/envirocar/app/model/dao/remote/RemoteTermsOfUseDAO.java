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
package org.envirocar.app.model.dao.remote;

import java.io.IOException;

import org.envirocar.app.model.dao.TermsOfUseDAO;
import org.envirocar.app.model.dao.cache.CacheTermsOfUseDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.TermsOfUseRetrievalException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteTermsOfUseDAO extends BaseRemoteDAO implements TermsOfUseDAO {

	private static final Logger logger = Logger.getLogger(RemoteTermsOfUseDAO.class);
	private CacheTermsOfUseDAO cache;

	public RemoteTermsOfUseDAO(CacheTermsOfUseDAO cacheTermsOfUseDAO) {
		this.cache = cacheTermsOfUseDAO;
	}

	@Override
	public TermsOfUse getTermsOfUse() throws TermsOfUseRetrievalException {
		try {
			JSONObject parentObject = readRemoteResource("/termsOfUse");
			
			if (cache != null) {
				try {
					cache.storeTermsOfUse(parentObject.toString());
				}
				catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			
			return TermsOfUse.fromJson(parentObject);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (NotConnectedException e) {
			throw new TermsOfUseRetrievalException(e);
		} catch (UnauthorizedException e) {
			throw new TermsOfUseRetrievalException(e);
		}
	}


	@Override
	public TermsOfUseInstance getTermsOfUseInstance(String id) throws TermsOfUseRetrievalException {
		try {
			JSONObject parentObject = readRemoteResource("/termsOfUse/"+id);
			
			if (cache != null) {
				try {
					cache.storeTermsOfUseInstance(parentObject.toString(), id);
				}
				catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			
			return TermsOfUseInstance.fromJson(parentObject);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new TermsOfUseRetrievalException(e);
		} catch (NotConnectedException e) {
			throw new TermsOfUseRetrievalException(e);
		} catch (UnauthorizedException e) {
			throw new TermsOfUseRetrievalException(e);
		}
	}

}
