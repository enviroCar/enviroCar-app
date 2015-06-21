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
import java.util.List;

import org.envirocar.app.model.dao.FuelingDAO;
import org.envirocar.app.model.dao.exception.FuelingRetrievalException;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.json.FuelingDecoder;
import org.envirocar.app.model.Fueling;
import org.json.JSONException;

public class CacheFuelingDAO extends AbstractCacheDAO implements FuelingDAO {



	private static final String FUELING_CACHE = "fuelings";

	@Override
	public void storeFueling(Fueling fueling) throws NotConnectedException {
		throw new NotConnectedException("CacheFuelingDAO does not support saving.");
	}

	@Override
	public List<Fueling> getFuelings() throws FuelingRetrievalException {
		try {
			return new FuelingDecoder().createListFromJson(readCache(FUELING_CACHE));
		} catch (IOException e) {
			throw new FuelingRetrievalException(e);
		} catch (JSONException e) {
			throw new FuelingRetrievalException(e);
		}
	}

}
