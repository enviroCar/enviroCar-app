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
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.ResourceConflictException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.model.User;
import org.envirocar.app.network.HTTPClient;

public abstract class BaseRemoteDAO {
	
	HttpResponse executeHttpRequest(HttpUriRequest request) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		if (this instanceof AuthenticatedDAO) {
			User user = UserManager.instance().getUser();
			
			if (user != null && user.getUsername() != null && user.getToken() != null) {
				request.addHeader("X-User", user.getUsername());
				request.addHeader("X-Token", user.getToken());	
			}
			
		}
		
		if (request instanceof HttpEntityEnclosingRequestBase) {
			if (!request.containsHeader("Content-Type")) {
				request.addHeader("Content-Type", "application/json");
			}
		}
		
		if (!request.containsHeader("Accept-Encoding")) {
			request.addHeader("Accept-Encoding", "gzip");
		}
		
		/*
		 * TODO enable client-site gzip if server responeded with that at least once!
		 */
		
		HttpResponse result;
		try {
			result = HTTPClient.execute(request);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		}
		assertStatusCode(result);
		
		return result;
	}
	
	public InputStream retrieveHttpContent(HttpUriRequest request) throws NotConnectedException, IOException, UnauthorizedException, ResourceConflictException {
		HttpResponse result = executeHttpRequest(request);
		
		if (result.containsHeader("Transfer-Encoding")) {
			String enc = result.getFirstHeader("Transfer-Encoding").getValue();
			if (enc.contains("gzip")) {
				return new GZIPInputStream(result.getEntity().getContent());
			}
		}
		
		if (result.containsHeader("Content-Encoding")) {
			String enc = result.getFirstHeader("Content-Encoding").getValue();
			if (enc.contains("gzip")) {
				return new GZIPInputStream(result.getEntity().getContent());
			}
		}
		
		return result.getEntity().getContent();
	}

	private void assertStatusCode(HttpResponse response) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		if (response == null || response.getStatusLine() == null) {
			throw new NotConnectedException("Unsupported server response.");
		}
		
		int httpStatusCode = response.getStatusLine().getStatusCode();
		
		if (httpStatusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
			String error = null;
			
			try {
				if (response.getEntity() != null && response.getEntity().getContentLength() > 0) {
					error = EntityUtils.toString(response.getEntity());
				}
			} catch (IllegalStateException e) {
				throw new NotConnectedException(e, httpStatusCode);
			} catch (ParseException e) {
				throw new NotConnectedException(e, httpStatusCode);
			} catch (IOException e) {
				throw new NotConnectedException(e, httpStatusCode);
			}
			
			if (httpStatusCode == HttpStatus.SC_UNAUTHORIZED ||
					httpStatusCode == HttpStatus.SC_FORBIDDEN) {
				throw new UnauthorizedException("Authentication failed: "+httpStatusCode +"; "+ error);
			}
			else if (httpStatusCode == HttpStatus.SC_CONFLICT) {
				throw new ResourceConflictException(error);
			}
			else {
				throw new NotConnectedException("Unsupported Server response: "+httpStatusCode +"; "+ error);
			}
		}
	}

}
