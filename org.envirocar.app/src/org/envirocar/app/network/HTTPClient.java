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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.envirocar.app.logging.Logger;

import android.net.http.AndroidHttpClient;

/**
 * Utility class to provide secure HTTP network access. 
 * 
 * @author matthes rieke
 *
 */
public class HTTPClient {

	private static final Logger logger = Logger.getLogger(HTTPClient.class);
	public static final int MIN_GZIP_SIZE = 8192;
	private static AndroidHttpClient client;
	
	static {
		createClient();
		setupClient(client);
	}
	
	/**
	 * execute a http request with a https-capable http client
	 * 
	 * @param request the http request
	 * @return the response, including status and content
	 * @throws IOException
	 */
	public static HttpResponse execute(HttpUriRequest request) throws IOException {
		AndroidHttpClient client = createClient();
		try {
			HttpResponse result = client.execute(request);
			return result;
		} catch (ClientProtocolException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * @return a https-capable http client
	 */
	protected synchronized static AndroidHttpClient createClient() {
		if (client == null) {
			client = AndroidHttpClient.newInstance("enviroCar-app");
			setupClient(client);
		}
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

	public static String executeAndParseJsonRequest(String url) throws IOException {
		return readResponse(executeJsonRequest(url));
	}
	
	public static HttpEntity executeJsonRequest(String url) throws IOException {
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader("Accept-Encoding", "application/json");
		
		HttpResponse response = HTTPClient.execute(getRequest);
		return response.getEntity();
	}

	public static HttpEntity createEntity(byte[] data) throws IOException {
        AbstractHttpEntity entity;
        if (data.length < MIN_GZIP_SIZE) {
            entity = new ByteArrayEntity(data);
        } else {
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            OutputStream zipper = new GZIPOutputStream(arr);
            zipper.write(data);
            zipper.close();
            entity = new ByteArrayEntity(arr.toByteArray());
            entity.setContentEncoding("gzip");
        }
        return entity;
	}
	
	public static synchronized void shutdown() {
		if (client != null) {
			client.close();
		}
	}
}
