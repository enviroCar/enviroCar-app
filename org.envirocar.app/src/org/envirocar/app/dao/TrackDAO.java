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
package org.envirocar.app.dao;

import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.storage.Track;

public interface TrackDAO {

	void deleteTrack(String remoteID) throws DAOException;
	
	void storeTrack(Track track) throws DAOException;
	
	List<Track> getAllTracks() throws NotConnectedException;
	
	Track getTrack(String id) throws NotConnectedException;

	Integer getUserTrackCount() throws NotConnectedException, TrackRetrievalException;
	
	Integer getTotalTrackCount() throws NotConnectedException, TrackRetrievalException;

	
	public static class TrackHelper {
		
		public static Integer resolveTrackCount(HttpResponse response) throws TrackRetrievalException {
			if (response.containsHeader("Link")) {
				Header[] link = response.getHeaders("Link");
				
				for (Header l : link) {
					Integer result = resolveLastRel(l.getValue());
					if (result != null) {
						return result;
					}
				}
				
				if (link.length > 0 && link[0].getValue() != null) {
					throw new TrackRetrievalException("Could not parse the HTTP Header 'Link': "+link[0].getValue());
				}
				else {
					throw new TrackRetrievalException("Invalid HTTP Header 'Link'");
				}
			}
			else {
				throw new TrackRetrievalException("Response did not contain the exepected HTTP Header 'Link'");
			}
			
		}

		public static Integer resolveLastRel(String value) {
			if (value != null) {
				String[] split = value.split(",");
				
				for (String line : split) {
					if (line.contains("rel=last")) {
						String[] params = line.split(";");
						if (params != null && params.length > 0) {
							return resolvePageValue(params[0]);
						}
					}
				}
			}
			return null;
		}

		public static Integer resolvePageValue(String sourceUrl) {
			String url;
			if (sourceUrl.startsWith("<")) {
				url = sourceUrl.substring(1, sourceUrl.length()-1);
			}
			else {
				url = sourceUrl;
			}
			
			if (url.contains("?")) {
				int index = url.indexOf("?")+1;
				if (index != url.length()) {
					String params = url.substring(index, url.length());
					for (String kvp : params.split("&")) {
						if (kvp.startsWith("page")) {
							return Integer.parseInt(kvp.substring(kvp.indexOf("page")+5));
						}
					}	
				}
			}
			return null;
		}
	}
}
