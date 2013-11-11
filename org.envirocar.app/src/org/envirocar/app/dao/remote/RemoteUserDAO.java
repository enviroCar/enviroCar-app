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

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.User;
import org.envirocar.app.dao.UserDAO;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.UserRetrievalException;
import org.envirocar.app.dao.exception.UserUpdateException;
import org.json.JSONException;

public class RemoteUserDAO extends BaseRemoteDAO implements UserDAO, AuthenticatedDAO {

	@Override
	public void updateUser(User user) throws UserUpdateException {
		HttpPut put = new HttpPut(ECApplication.BASE_URL+"/users/"+user.getUsername());
		try {
			put.setEntity(new StringEntity(user.toJson()));
			executeHttpRequest(put);
		} catch (UnsupportedEncodingException e) {
			throw new UserUpdateException(e);
		} catch (JSONException e) {
			throw new UserUpdateException(e);
		} catch (NotConnectedException e) {
			throw new UserUpdateException(e);
		}
	}

	@Override
	public User getUser(String id) throws UserRetrievalException {
		
		return null;
	}

}
