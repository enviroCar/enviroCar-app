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
package org.envirocar.app.dao.cache;

import java.util.List;

import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.storage.Track;

public class CacheTrackDAO implements TrackDAO {

	@Override
	public void deleteTrack(String remoteID) throws NotConnectedException {
		throw new NotConnectedException("Not implemented for Cache DAO");		
	}

	@Override
	public void storeTrack(Track track) throws NotConnectedException {
		throw new NotConnectedException("Not implemented for Cache DAO");
	}

	@Override
	public List<Track> getAllTracks() throws NotConnectedException {
		throw new NotConnectedException("Not implemented for Cache DAO");
	}

	@Override
	public Track getTrack(String id) throws NotConnectedException {
		throw new NotConnectedException("Not implemented for Cache DAO");
	}

	@Override
	public Integer getUserTrackCount() throws NotConnectedException,
			TrackRetrievalException {
		throw new NotConnectedException("Not implemented for Cache DAO");
	}

	@Override
	public Integer getTotalTrackCount() throws NotConnectedException,
			TrackRetrievalException {
		throw new NotConnectedException("Not implemented for Cache DAO");
	}

}
