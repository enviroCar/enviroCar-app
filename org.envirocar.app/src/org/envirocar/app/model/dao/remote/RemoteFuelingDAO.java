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
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.envirocar.app.ConstantsEnvirocar;
import org.envirocar.app.model.dao.FuelingDAO;
import org.envirocar.app.model.dao.cache.CacheFuelingDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.exception.InvalidObjectStateException;
import org.envirocar.app.json.FuelingEncoder;
import org.envirocar.app.model.Fueling;
import org.json.JSONException;

public class RemoteFuelingDAO extends BaseRemoteDAO implements FuelingDAO, AuthenticatedDAO {

	private CacheFuelingDAO cache;

	public RemoteFuelingDAO(CacheFuelingDAO cacheFuelingDAO) {
		this.cache = cacheFuelingDAO;
	}

	@Override
	public void storeFueling(Fueling fueling) throws NotConnectedException, InvalidObjectStateException {
		String user = mUserManager.getUser().getUsername();
		HttpPost post = new HttpPost(ConstantsEnvirocar.BASE_URL+"/users/"+user+"/fuelings");
		
		try {
			post.setEntity(super.preparePayload(new FuelingEncoder().createFuelingJson(fueling).toString()));
			super.executePayloadRequest(post);
		} catch (UnauthorizedException e) {
			throw new NotConnectedException(e);
		} catch (ResourceConflictException e) {
			throw new NotConnectedException(e);
		} catch (UnsupportedEncodingException e) {
			throw new NotConnectedException(e);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		} catch (JSONException e) {
			throw new NotConnectedException(e);
		}
	}

	@Override
	public List<Fueling> getFuelings() {
		// TODO implement
		return Collections.emptyList();
	}

}
