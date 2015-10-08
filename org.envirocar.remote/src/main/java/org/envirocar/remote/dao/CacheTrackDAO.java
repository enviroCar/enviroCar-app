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
package org.envirocar.remote.dao;

import org.envirocar.core.dao.TrackDAO;
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
public class CacheTrackDAO implements TrackDAO {

    @Override
    public Track getTrackById(String id) throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public Observable<Track> getTrackByIdObservable(String id) {
        return Observable.error(new NotConnectedException("Not implemented for Cache DAO"));
    }

    @Override
    public List<Track> getTrackIds() throws DataRetrievalFailureException, NotConnectedException,
            UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public List<Track> getTrackIds(int limit, int page) throws NotConnectedException,
            UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable() {
        return Observable.error(new NotConnectedException("Not implemented for Cache DAO"));
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable(int limit, int page) {
        return Observable.error(new NotConnectedException("Not implemented for Cache DAO"));
    }

    @Override
    public Integer getUserTrackCount() throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public Integer getTotalTrackCount() throws DataRetrievalFailureException,
            NotConnectedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public String createTrack(Track track) throws DataCreationFailureException,
            NotConnectedException, ResourceConflictException, UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public void deleteTrack(String remoteID) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {
        throw new NotConnectedException("Not implemented for Cache DAO");
    }

    @Override
    public void deleteTrack(Track track) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {

    }
}
