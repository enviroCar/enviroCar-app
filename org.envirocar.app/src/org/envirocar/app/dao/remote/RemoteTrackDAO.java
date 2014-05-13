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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.ResourceConflictException;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.dao.exception.TrackSerializationException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.json.TrackDecoder;
import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.model.User;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.FinishedTrackWithoutMeasurementsException;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteTrackDAO extends BaseRemoteDAO implements TrackDAO, AuthenticatedDAO {

	@Override
	public void deleteTrack(String remoteID) throws UnauthorizedException, NotConnectedException {
		User user = UserManager.instance().getUser();
		
		if (user == null) {
			throw new UnauthorizedException("No User logged in.");
		}
		
		HttpDelete request = new HttpDelete(ECApplication.BASE_URL+"/users/"+
				user.getUsername()+"/tracks/" + remoteID);

		super.executeContentRequest(request);
	}

	@Override
	public String storeTrack(Track track, boolean obfuscate) throws NotConnectedException, FinishedTrackWithoutMeasurementsException, TrackSerializationException, TrackRetrievalException {
		try {
			JSONObject content = new TrackEncoder().createTrackJson(track, obfuscate);
			
			User user = UserManager.instance().getUser();
			HttpPost post = new HttpPost(String.format("%s/users/%s/tracks", ECApplication.BASE_URL,
					user.getUsername()));
			
			HttpResponse response = executePayloadRequest(post, content.toString());
			
			return new TrackDecoder().resolveLocation(response);
		} catch (JSONException e) {
			throw new TrackSerializationException(e);
		} catch (ResourceConflictException e) {
			throw new NotConnectedException(e);
		} catch (UnauthorizedException e) {
			throw new TrackRetrievalException(e);
		}
	}


	@Override
	public Track getTrack(String id) throws NotConnectedException {
		Track result;
		try {
			JSONObject parentObject = readRemoteResource("/tracks/"+id);
			result = new TrackDecoder().fromJson(parentObject);
		} catch (ParseException e) {
			throw new NotConnectedException(e);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		} catch (JSONException e) {
			throw new NotConnectedException(e);
		} catch (java.text.ParseException e) {
			throw new NotConnectedException(e);
		} catch (UnauthorizedException e) {
			throw new NotConnectedException(e);
		}
		
		return result;
	}

	@Override
	public Integer getUserTrackCount() throws NotConnectedException, TrackRetrievalException {
		User user = UserManager.instance().getUser();
		HttpGet get = new HttpGet(ECApplication.BASE_URL+"/users/"+user.getUsername()+"/tracks?limit=1");
		
		HttpResponse response;
		try {
			response = super.executeContentRequest(get);
		} catch (UnauthorizedException e) {
			throw new TrackRetrievalException(e);
		}
		return new TrackDecoder().resolveTrackCount(response);
	}

	@Override
	public Integer getTotalTrackCount() throws NotConnectedException, TrackRetrievalException {
		HttpGet get = new HttpGet(ECApplication.BASE_URL+"/tracks?limit=1");
		
		HttpResponse response;
		try {
			response = super.executeContentRequest(get);
		} catch (UnauthorizedException e) {
			throw new TrackRetrievalException(e);
		}
		return new TrackDecoder().resolveTrackCount(response);
	}

	@Override
	public List<String> getTrackIds() throws NotConnectedException, UnauthorizedException {
		return getTrackIds(100);
	}

	@Override
	public List<String> getTrackIds(int limit) throws NotConnectedException, UnauthorizedException {
		return getTrackIds(limit, 1);
	}

	@Override
	public List<String> getTrackIds(int limit, int page) throws NotConnectedException, UnauthorizedException {
		User user = UserManager.instance().getUser();
		HttpGet get = new HttpGet(String.format("%s/users/%s/tracks?limit=%d&page=%d",
				ECApplication.BASE_URL, user.getUsername(), limit, page));
		
		InputStream response;
		try {
			response = super.retrieveHttpContent(get);
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
