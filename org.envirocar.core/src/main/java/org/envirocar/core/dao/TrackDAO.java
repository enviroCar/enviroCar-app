/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.dao;

import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;

import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TrackDAO {

    Track getTrackById(String id) throws DataRetrievalFailureException, NotConnectedException,
            UnauthorizedException;

    Observable<Track> getTrackByIdObservable(String id);

    List<Track> getTrackIds() throws DataRetrievalFailureException, NotConnectedException,
            UnauthorizedException;

    List<Track> getTrackIds(int limit, int page) throws NotConnectedException,
            UnauthorizedException;

    Observable<List<Track>> getTrackIdsObservable();

    Observable<List<Track>> getTrackIdsObservable(int limit, int page);

    Integer getUserTrackCount() throws DataRetrievalFailureException, NotConnectedException,
            UnauthorizedException;

    Integer getTotalTrackCount() throws DataRetrievalFailureException, NotConnectedException;

    Track createTrack(Track track) throws DataCreationFailureException, NotConnectedException,
            ResourceConflictException, UnauthorizedException;

    Observable<Track> createTrackObservable(Track track);

    void deleteTrack(Track track) throws DataUpdateFailureException, NotConnectedException,
            UnauthorizedException;
}
