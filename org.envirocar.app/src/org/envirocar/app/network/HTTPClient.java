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

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.envirocar.app.logging.Logger;

/**
 * Utility class to provide secure HTTP network access. 
 * 
 * @author matthes rieke
 *
 */
public class HTTPClient {

	private static final Logger logger = Logger.getLogger(HTTPClient.class);
	
	/**
	 * execute a http request with a https-capable http client
	 * 
	 * @param request the http request
	 * @return the response, including status and content
	 * @throws IOException
	 */
	public static HttpResponse execute(HttpUriRequest request) throws IOException {
		HttpClient client = createClient();
		try {
			return client.execute(request);
		} catch (ClientProtocolException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * @return a https-capable http client
	 */
	protected static HttpClient createClient() {
		HttpClient client = new DefaultHttpClient();
		setupClient(client);
		return client;
	}

	/**
	 * Convenience method to consume the contents of an entity.
	 * 
	 * @param entity content-holding entity
	 */
	public static void consumeEntity(HttpEntity entity) {
		try {
			if (entity == null || entity.getContent() == null) return;
			entity.consumeContent();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}		
	}

	/**
	 * setup a client instance with SSL/HTTPS capabilities.
	 * 
	 * @param client the client to set up
	 */
	public static void setupClient(HttpClient client) {
		SSLSocketFactory factory = SSLSocketFactory.getSocketFactory();
		factory.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
		client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", factory, 443));		
	}

	public static String readResponse(HttpEntity entity) throws IOException {
//		if (entity == null || entity.getContent() == null || entity.getContentLength() == 0)
//			return null;
//		
//		StringBuilder sb = new StringBuilder();
//		
//		Scanner sc = new Scanner(entity.getContent());
//		while (sc.hasNext()) {
//			sb.append(sc.nextLine());
//			sb.append(System.getProperty("line.separator"));
//		}
//		sc.close();
//		
		return EntityUtils.toString(entity, HTTP.UTF_8);
	}
	
}
