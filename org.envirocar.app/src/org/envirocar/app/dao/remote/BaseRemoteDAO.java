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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.NotConnectedException;
import org.envirocar.app.network.HTTPClient;

public abstract class BaseRemoteDAO {
	
	HttpResponse executeHttpRequest(HttpUriRequest request) throws NotConnectedException {
		if (this instanceof AuthenticatedDAO) {
			User user = UserManager.instance().getUser();
			
			if (user != null) {
				request.addHeader("X-User", user.getUsername());
				request.addHeader("X-Token", user.getToken());	
			}
			
		}
		
		HttpResponse result;
		try {
			result = HTTPClient.execute(request);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		}
		assertStatusCode(result);
		
		return result;
	}
	
	private void assertStatusCode(HttpResponse response) throws NotConnectedException {
		if (response == null || response.getStatusLine() == null) {
			throw new NotConnectedException("Unsupported server response.");
		}
		
		int httpStatusCode = response.getStatusLine().getStatusCode();
		
		if (httpStatusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
			if (httpStatusCode == HttpStatus.SC_UNAUTHORIZED ||
					httpStatusCode == HttpStatus.SC_FORBIDDEN) {
				throw new UnauthorizedException("Authentication failed.");
			}
			else {
				throw new NotConnectedException("Unsupported server response.");
			}
		}
	}

}
