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
package org.envirocar.app.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TrackDecoder {

	public Integer resolveTrackCount(HttpResponse response) throws TrackRetrievalException {
		try {
			return Util.resolveResourceCount(response);
		} catch (ServerException e) {
			throw new TrackRetrievalException(e);
		}
	}

	public List<String> getResourceIds(InputStream response) throws ParseException, IOException, JSONException {
		String result = Util.consumeInputStream(response).toString();
		
		JSONObject json = new JSONObject(result);
		
		JSONArray tracks = json.getJSONArray("tracks");
		
		
		if (tracks != null && tracks.length() > 0) {
			List<String> list = new ArrayList<String>(tracks.length());
			
			JSONObject o;
			for (int i = 0; i < tracks.length(); i++) {
				o = tracks.getJSONObject(i);
				list.add(o.getString("id"));
			}
			
			return list;
		}
		
		return Collections.emptyList();
	}

	public Track fromJson(HttpEntity content) throws ParseException, IOException, JSONException, java.text.ParseException {
		String result = EntityUtils.toString(content);
		return fromJson(new JSONObject(result));
	}

	public Track fromJson(JSONObject json) throws JSONException, java.text.ParseException {
		return Track.fromJson(json, DbAdapterImpl.instance());
	}
	
	public Track fromJson(InputStream jsonStream) throws JSONException, java.text.ParseException, IOException {
		/*
		 * TODO use GSON or the like to efficiently parse Streams
		 */
		CharSequence contents = Util.consumeInputStream(jsonStream);
		return fromJson(contents);
	}

	public Track fromJson(CharSequence contents) throws JSONException, java.text.ParseException {
		return fromJson(new JSONObject(contents.toString()));
	}

	public String resolveLocation(HttpResponse response) throws TrackRetrievalException {
		String location = null;
		Header h = response.getFirstHeader("Location");

		if (h != null) {
			location = h.getValue();
		}
		else {
			throw new TrackRetrievalException("Could not parse the 'Location' Header.");
		}
		
		String trackid = location.substring(location.lastIndexOf("/")+1, location.length());
		return trackid;
	}
	
}
