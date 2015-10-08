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

    String createTrack(Track track) throws DataCreationFailureException, NotConnectedException,
            ResourceConflictException, UnauthorizedException;

    void deleteTrack(String remoteID) throws DataUpdateFailureException, NotConnectedException,
            UnauthorizedException;

    void deleteTrack(Track track) throws DataUpdateFailureException, NotConnectedException,
            UnauthorizedException;
}
