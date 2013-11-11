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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.dao.exception.TrackSerializationException;
import org.envirocar.app.json.TrackDecoder;
import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.model.User;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteTrackDAO extends BaseRemoteDAO implements TrackDAO, AuthenticatedDAO {

	@Override
	public void deleteTrack(String remoteID) throws NotLoggedInException, NotConnectedException {
		User user = UserManager.instance().getUser();
		
		if (user == null) {
			throw new NotLoggedInException();
		}
		
		HttpDelete request = new HttpDelete(ECApplication.BASE_URL+"/users/"+
				user.getUsername()+"/tracks/" + remoteID);

		super.executeHttpRequest(request);
	}

	@Override
	public String storeTrack(Track track, boolean obfuscate) throws NotConnectedException, TrackWithoutMeasurementsException, TrackSerializationException, TrackRetrievalException {
		try {
			JSONObject content = new TrackEncoder().createTrackJson(track, obfuscate);
			
			User user = UserManager.instance().getUser();
			HttpPost post = new HttpPost(String.format("%s/users/%s/tracks", ECApplication.BASE_URL,
					user.getUsername()));
			try {
				post.setEntity(new StringEntity(content.toString()));
			} catch (UnsupportedEncodingException e) {
				throw new TrackSerializationException(e);
			}
			
			HttpResponse response = executeHttpRequest(post);
			
			return new TrackDecoder().resolveLocation(response);
		} catch (JSONException e) {
			throw new TrackSerializationException(e);
		}
	}


	@Override
	public Track getTrack(String id) throws NotConnectedException {
		HttpGet get = new HttpGet(ECApplication.BASE_URL+"/tracks/"+id);
		
		InputStream response;
		try {
			response = retrieveHttpContent(get);
		} catch (IllegalStateException e1) {
			throw new NotConnectedException(e1);
		} catch (IOException e1) {
			throw new NotConnectedException(e1);
		}
		
		Track result;
		try {
			result = new TrackDecoder().fromJson(response);
		} catch (ParseException e) {
			throw new NotConnectedException(e);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		} catch (JSONException e) {
			throw new NotConnectedException(e);
		} catch (java.text.ParseException e) {
			throw new NotConnectedException(e);
		}
		
		return result;
	}

	@Override
	public Integer getUserTrackCount() throws NotConnectedException, TrackRetrievalException {
		User user = UserManager.instance().getUser();
		HttpGet get = new HttpGet(ECApplication.BASE_URL+"/users/"+user.getUsername()+"/tracks?limit=1");
		
		HttpResponse response = executeHttpRequest(get);
		return new TrackDecoder().resolveTrackCount(response);
	}

	@Override
	public Integer getTotalTrackCount() throws NotConnectedException, TrackRetrievalException {
		HttpGet get = new HttpGet(ECApplication.BASE_URL+"/tracks?limit=1");
		
		HttpResponse response = executeHttpRequest(get);
		
		return new TrackDecoder().resolveTrackCount(response);
	}

	@Override
	public List<String> getTrackIds() throws NotConnectedException {
		return getTrackIds(100);
	}

	@Override
	public List<String> getTrackIds(int limit) throws NotConnectedException {
		return getTrackIds(limit, 1);
	}

	@Override
	public List<String> getTrackIds(int limit, int page) throws NotConnectedException {
		User user = UserManager.instance().getUser();
		HttpGet get = new HttpGet(String.format("%s/users/%s/tracks?limit=%d&page=%d",
				ECApplication.BASE_URL, user.getUsername(), limit, page));
		
		InputStream response;
		try {
			response = retrieveHttpContent(get);
		} catch (IllegalStateException e1) {
			throw new NotConnectedException(e1);
		} catch (IOException e1) {
			throw new NotConnectedException(e1);
		}
		
		List<String> result;
		try {
			result = new TrackDecoder().getResourceIds(response);
		} catch (ParseException e) {
			throw new NotConnectedException(e);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		} catch (JSONException e) {
			throw new NotConnectedException(e);
		}
		
		return result;
	}



}
