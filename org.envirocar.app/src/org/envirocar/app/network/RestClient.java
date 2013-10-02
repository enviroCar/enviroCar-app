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

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.logging.Logger;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
//TODO javadoc
public class RestClient {
	
	private static final Logger logger = Logger.getLogger(RestClient.class);
	
	private static AsyncHttpClient client;
	
	static {
		client = new AsyncHttpClient();
		HTTPClient.setupClient(client.getHttpClient());
	}
	
	public static void downloadTracks(String user, String token, JsonHttpResponseHandler handler){
		get(ECApplication.BASE_URL+"/users/"+user+"/tracks?limit=5&page=0", handler, user, token);
	}
	
	public static void deleteRemoteTrack(String user, String token, String id, JsonHttpResponseHandler handler){
		setHeaders(user, token);
		client.delete(ECApplication.BASE_URL+"/users/"+user+"/tracks/" + id, handler);
	}
	
	public static void downloadTrack(String user, String token, String id, JsonHttpResponseHandler handler){
		get(ECApplication.BASE_URL+"/tracks/"+id, handler, user, token);
	}
	
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
	
	private static void get(String url, JsonHttpResponseHandler handler, String user, String token) {		
		setHeaders(user, token);
		
		client.get(url, handler);
	}

	private static void setHeaders(String user, String token) {
		client.addHeader("Accept-Encoding", "gzip");
		
		if (user != null)
			client.addHeader("X-User", user);
		
		if (token != null)
			client.addHeader("X-Token", token);
		
	}

	public static void downloadSensors(JsonHttpResponseHandler handler){
		get(ECApplication.BASE_URL+"/sensors", handler);
	}

	private static void get(String url, JsonHttpResponseHandler handler) {
		get(url, handler, null, null);
	}

	public static void downloadTermsOfUse(JsonHttpResponseHandler handler) {
		String url = "http://geoprocessing.demo.52north.org:8081/xyz/termsOfUse.json";
//		String url = ECApplication.BASE_URL+"/termsOfUse";
		get(url, handler);
	}

	public static void downloadTermsOfUseInstance(String id,
			JsonHttpResponseHandler handler) {
		String url = "http://geoprocessing.demo.52north.org:8081/xyz/termsOfUseInstance.json";
//		String url = ECApplication.BASE_URL+"/termsOfUse/+id";
		get(url, handler);
	}
}
