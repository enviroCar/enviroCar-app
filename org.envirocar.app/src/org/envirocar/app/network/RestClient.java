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

package org.envirocar.app.network;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.User;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.logging.Logger;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
//TODO javadoc

public class RestClient {
	
	private static final Logger logger = Logger.getLogger(RestClient.class);
	
	private static AsyncHttpClient client;
	
	static {
		resetClient();
	}
	
	/**
	 * start downloading the latest 5 tracks of the given user.
	 * 
	 * @param user the user
	 * @param token the users token
	 * @param limit the maximum track count
	 * @param page the page (/tracks/ is a paging-enabled resource)
	 * @param handler called on success or failure
	 */
	public static void downloadTracks(String user, String token, int limit, int page, JsonHttpResponseHandler handler){
		get(String.format(Locale.ENGLISH, "%s/users/%s/tracks?limit=%d&page=%d", ECApplication.BASE_URL, user, limit, page),
			handler, user, token);
	}
	
	/**
	 * start downloading the latest 5 tracks of the given user.
	 * 
	 * @param user the user
	 * @param token the users token
	 * @param handler called on success or failure
	 */
	public static void downloadTracks(String user, String token, JsonHttpResponseHandler handler){
		downloadTracks(user, token, 5, 1, handler);
	}
	
	private static void resetClient() {
		client = new AsyncHttpClient();
		HTTPClient.setupClient(client.getHttpClient());		
	}

	@Deprecated
	public static void deleteRemoteTrack(String user, String token, String id, JsonHttpResponseHandler handler){
		setHeaders(user, token);
		client.delete(ECApplication.BASE_URL+"/users/"+user+"/tracks/" + id, handler);
	}
	
	public static void downloadTrack(String user, String token, String id, AsyncHttpResponseHandler handler){
		get(ECApplication.BASE_URL+"/tracks/"+id, handler, user, token);
	}
	
	/**
	 * @deprecated Use {@link DAOProvider#getSensorDAO()} / {@link SensorDAO#saveSensor(org.envirocar.app.model.Car, User)}
	 * instead.
	 */
	@Deprecated
	public static boolean createSensor(String jsonObj, String user, String token, AsyncHttpResponseHandler handler){
		client.addHeader("Content-Type", "application/json");
		setHeaders(user, token);
		
		try {
			StringEntity se = new StringEntity(jsonObj);
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			client.post(null, ECApplication.BASE_URL+"/sensors", se, "application/json", handler);		  
		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	private static void get(String url, AsyncHttpResponseHandler handler, String user, String token) {		
		setHeaders(user, token);
		
		client.get(url, handler);
	}
	
	private static void put(String url, AsyncHttpResponseHandler handler, String contents, String user, String token) throws UnsupportedEncodingException {		
		client.addHeader("Content-Type", "application/json");
		setHeaders(user, token);
		
		StringEntity se = new StringEntity(contents);
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

		client.put(null, url, se, "application/json", handler);
	}

	private static void setHeaders(String user, String token) {
		client.addHeader("Accept-Encoding", "gzip");
		
		if (user != null)
			client.addHeader("X-User", user);
		
		if (token != null)
			client.addHeader("X-Token", token);
		
	}

	@Deprecated
	public static void downloadSensors(JsonHttpResponseHandler handler){
		get(ECApplication.BASE_URL+"/sensors", handler);
	}

	private static void get(String url, JsonHttpResponseHandler handler) {
		get(url, handler, null, null);
	}


	@Deprecated
	public static void updateAcceptedTermsOfUseVersion(User user,
			String issuedDate, AsyncHttpResponseHandler handler) {
		String contents = String.format("{\"%s\": \"%s\"}", "acceptedTermsOfUseVersion", issuedDate);
		try {
			put(ECApplication.BASE_URL+"/users/"+user.getUsername(), handler, contents, user.getUsername(), user.getToken());
		} catch (UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public static void removeUserSpecificHeaders() {
		resetClient();
	}
}
