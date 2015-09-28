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
package org.envirocar.app.model.dao;

import java.util.List;

import org.envirocar.app.model.dao.exception.DAOException;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.TrackRetrievalException;
import org.envirocar.app.model.dao.exception.TrackSerializationException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.storage.Track;

import rx.Observable;

public interface TrackDAO {

	void deleteTrack(String remoteID) throws DAOException;

	String storeTrack(Track track, boolean obfuscate)
			throws NotConnectedException,
			TrackSerializationException, TrackRetrievalException, TrackWithoutMeasurementsException, UnauthorizedException;

	Track getTrack(String id) throws NotConnectedException;

	Integer getUserTrackCount() throws NotConnectedException,
			TrackRetrievalException;

	Integer getTotalTrackCount() throws NotConnectedException,
			TrackRetrievalException;


	/**
	 * an implementation shall treat calls as a shortcut for
	 * {@link #getTrackIds(int)} with limit=100
	 * 
	 * @return the resource IDs of the desired tracks
	 * @throws NotConnectedException
	 * @throws UnauthorizedException 
	 */
	List<String> getTrackIds() throws NotConnectedException, UnauthorizedException;

	/**
	 * an implementation shall treat calls as a shortcut for
	 * {@link #getTrackIds(int, int)} with limit=limit and page=1
	 * 
	 * @param limit
	 *            the total count of returned track ids
	 * @return the resource IDs of the desired tracks
	 * @throws NotConnectedException
	 * @throws UnauthorizedException 
	 */
	List<String> getTrackIds(int limit) throws NotConnectedException, UnauthorizedException;

	/**
	 * @param limit
	 *            the total count of returned track ids
	 * @param page
	 *            the pagination index (starting at 1)
	 * @return the resource IDs of the desired tracks
	 * @throws NotConnectedException
	 * @throws UnauthorizedException 
	 */
	List<String> getTrackIds(int limit, int page) throws NotConnectedException, UnauthorizedException;

}
