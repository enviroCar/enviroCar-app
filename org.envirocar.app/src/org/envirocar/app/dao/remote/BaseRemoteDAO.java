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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.ResourceConflictException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.model.User;
import org.envirocar.app.network.HTTPClient;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;


public abstract class BaseRemoteDAO {
	
	private void assertStatusCode(HttpResponse response) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		if (response == null || response.getStatusLine() == null) {
			throw new NotConnectedException("Unsupported server response.");
		}
		
		int httpStatusCode = response.getStatusLine().getStatusCode();
		
		if (httpStatusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
			String error = null;
			
			try {
				if (response.getEntity() != null) {
					error = HTTPClient.readResponse(response.getEntity());
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
	
	
	private HttpResponse executeHttpRequest(HttpUriRequest request) throws NotConnectedException {
		if (this instanceof AuthenticatedDAO) {
			User user = UserManager.instance().getUser();
			
			if (user != null && user.getUsername() != null && user.getToken() != null) {
				request.addHeader("X-User", user.getUsername());
				request.addHeader("X-Token", user.getToken());	
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
		
		return result;
	}
	
	/**
	 * Reads a remote REST resource
	 * 
	 * @param remoteRestResource the sub resource
	 * @return the remote resource encoded as a {@link JSONObject}
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 * @throws IOException
	 * @throws JSONException
	 */
	protected JSONObject readRemoteResource(String remoteRestResource) throws NotConnectedException, UnauthorizedException, IOException, JSONException {
		HttpGet get = new HttpGet(ECApplication.BASE_URL+remoteRestResource);
		InputStream response = retrieveHttpContent(get);
		String content = Util.consumeInputStream(response).toString();
	
		JSONObject parentObject = new JSONObject(content);
		return parentObject;
	}
	
	/**
	 * Reads a remote REST resource
	 * 
	 * @param remoteRestResource the sub resource
	 * @param complete if the complete resource (all pages) should be read
	 * @return the remote resource encoded as a {@link JSONObject}
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 * @throws IOException
	 * @throws JSONException
	 */
	protected List<JSONObject> readRemoteResource(String remoteRestResource, boolean complete) throws NotConnectedException, UnauthorizedException, IOException, JSONException {
		if (!complete) {
			return Collections.singletonList(readRemoteResource(remoteRestResource));
		}
		
		HttpGet get = new HttpGet(ECApplication.BASE_URL+remoteRestResource+"?limit=100");
		HttpResponse response = executeContentRequest(get);
		
		Integer count;
		try {
			count = Util.resolveResourceCount(response);
		} catch (ServerException e) {
			throw new IOException(e);
		}
		
		if (count > 1) {
			List<JSONObject> result = new ArrayList<JSONObject>(count);
			
			for (int i = 1; i <= count; i++) {
				result.add(readRemoteResource(remoteRestResource+"?limit=100&page="+i));
			}
			
			return result;
		}
		else {
			return Collections.singletonList(readRemoteResource(remoteRestResource));
		}
	}
	
	/**
	 * execute a request for remote content (e.g. GET) or a request
	 * which does not expect contents back (e.g. DELETE)
	 * 
	 * @param request the request
	 * @return the response object, containing headers and content
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 */
	protected HttpResponse executeContentRequest(HttpUriRequest request) throws NotConnectedException, UnauthorizedException {
		HttpResponse result = executeHttpRequest(request);
		
		try {
			assertStatusCode(result);
		} catch (ResourceConflictException e) {
			throw new NotConnectedException(e);
		}
		
		return result;
	}
	
	/**
	 * execute a request which carries payload (e.g. POST, PUT).
	 * 
	 * @param request the request
	 * @param payload the payload as {@link CharSequence}
	 * @return the response object, containing headers and content
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 * @throws ResourceConflictException
	 */
	protected HttpResponse executePayloadRequest(HttpEntityEnclosingRequestBase request, CharSequence payload) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		try {
			request.setEntity(preparePayload(payload));
		} catch (UnsupportedEncodingException e) {
			throw new NotConnectedException(e);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		}
		return executePayloadRequest(request);
	}
	
	/**
	 * execute a request which carries payload (e.g. POST, PUT).
	 * 
	 * @param request
	 * @param payload the payload as byte array
	 * @return the response object, containing headers and content
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 * @throws ResourceConflictException
	 */
	protected HttpResponse executePayloadRequest(HttpEntityEnclosingRequestBase request, byte[] payload) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		try {
			request.setEntity(preparePayload(payload));
		} catch (IOException e) {
			throw new NotConnectedException(e);
		}
		return executePayloadRequest(request);
	}
	
	/**
	 * execute a request which carries payload (e.g. POST, PUT).
	 * 
	 * @param request the request, containing the payload as its {@link HttpEntity}
	 * @return the response object, containing headers and content
	 * @throws NotConnectedException
	 * @throws UnauthorizedException
	 * @throws ResourceConflictException
	 */
	protected HttpResponse executePayloadRequest(HttpEntityEnclosingRequestBase request) throws NotConnectedException, UnauthorizedException, ResourceConflictException {
		if (!request.containsHeader("Content-Type")) {
			request.addHeader("Content-Type", "application/json");
		}
		
		HttpResponse result = executeHttpRequest(request);
		
		assertStatusCode(result);
		
		return result;
	}

	/**
	 * Wrap content into an HTTP entity. a GZIP entity is used if
	 * the content length exceeds {@link HTTPClient#MIN_GZIP_SIZE}.
	 * 
	 * @param content the content to wrap
	 * @return the entity
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	protected HttpEntity preparePayload(CharSequence content) throws UnsupportedEncodingException, IOException {
		return preparePayload(content.toString().getBytes("UTF-8"));
	}
	
	/**
	 * Wrap content into an HTTP entity. a GZIP entity is used if
	 * the content length exceeds {@link HTTPClient#MIN_GZIP_SIZE}.
	 * 
	 * @param content the content to wrap
	 * @return the entity
	 * @throws IOException
	 */
	protected HttpEntity preparePayload(byte[] content) throws IOException {
		return HTTPClient.createEntity(content);
	}
	
	/**
	 * executes a request and only returns its contents (i.e. no headers are available).
	 * 
	 * @param request the request
	 * @return the content as {@link InputStream}
	 * @throws NotConnectedException
	 * @throws IOException
	 * @throws UnauthorizedException
	 */
	protected InputStream retrieveHttpContent(HttpUriRequest request) throws NotConnectedException, IOException, UnauthorizedException {
		HttpResponse result = executeContentRequest(request);
		
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

}
