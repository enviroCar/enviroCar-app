package org.envirocar.storage;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;

import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface EnviroCarDB {

    Observable<Track> getTrack(Track.TrackId trackId);

    Observable<Track> getTrack(Track.TrackId trackId, boolean lazy);

    /**
     * Returns an observable providing all tracks as an {@link List}.
     *
     * @return All tracks as observable
     */
    Observable<List<Track>> getAllTracks();

    /**
     * Returns an observable providing all tracks as an {@link List}.
     *
     * @param lazy indicates whether the measurements should be loaded or not.
     * @return all tracks as observable.
     */
    Observable<List<Track>> getAllTracks(boolean lazy);

    Observable<List<Track>> getAllLocalTracks();

    Observable<List<Track>> getAllLocalTracks(boolean lazy);

    Observable<List<Track>> getAllRemoteTracks();

    Observable<List<Track>> getAllRemoteTracks(boolean lazy);

    Observable<Void> clearTables();

    Observable<Void> insertTrack(Track track);

    Observable<Void> insertMeasurement(Measurement measurement);


}