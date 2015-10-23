package org.envirocar.storage.dao;

import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.storage.EnviroCarDB;

import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LocalTrackDAO implements TrackDAO {

//    @Inject
    protected EnviroCarDB database;

    @Override
    public Track getTrackById(String id) throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        return database.getTrack(new Track.TrackId(Long.parseLong(id)))
                .take(1)
                .toBlocking()
                .first();
    }

    @Override
    public Observable<Track> getTrackByIdObservable(String id) {
        return database.getTrack(new Track.TrackId(Long.parseLong(id)))
                .take(1);
    }

    @Override
    public List<Track> getTrackIds() throws DataRetrievalFailureException, NotConnectedException,
            UnauthorizedException {
        return null;
    }

    @Override
    public List<Track> getTrackIds(int limit, int page) throws NotConnectedException,
            UnauthorizedException {
        return null;
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable() {
        return null;
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable(int limit, int page) {
        return null;
    }

    @Override
    public Integer getUserTrackCount() throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        return null;
    }

    @Override
    public Integer getTotalTrackCount() throws DataRetrievalFailureException,
            NotConnectedException {
        return null;
    }

    @Override
    public String createTrack(Track track) throws DataCreationFailureException,
            NotConnectedException, ResourceConflictException, UnauthorizedException {
        return null;
    }

    @Override
    public void deleteTrack(String remoteID) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {

    }

    @Override
    public void deleteTrack(Track track) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {

    }
}
