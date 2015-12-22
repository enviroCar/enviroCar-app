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
package org.envirocar.storage;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.util.TrackMetadata;

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

    Observable<List<Track>> getAllTracksByCar(String id, boolean lazy);

    Observable<List<Track>> getAllLocalTracks();

    Observable<List<Track>> getAllLocalTracks(boolean lazy);

    Observable<List<Track>> getAllRemoteTracks();

    Observable<List<Track>> getAllRemoteTracks(boolean lazy);

    Observable<Void> clearTables();

    void insertTrack(Track track) throws TrackSerializationException;

    Observable<Track> insertTrackObservable(Track track);

    boolean updateTrack(Track track);

    Observable<Track> updateTrackObservable(Track track);

    boolean updateCarIdOfTracks(String currentId, String newId);

    void deleteTrack(Track.TrackId trackId);

    void deleteTrack(Track track);

    Observable<Void> deleteTrackObservable(Track track);

    Observable<Void> deleteAllRemoteTracks();

    void insertMeasurement(Measurement measurement) throws MeasurementSerializationException;

    Observable<Void> insertMeasurementObservable(Measurement measurement);

    void updateTrackRemoteID(Track track, String remoteID);

    Observable<Void> updateTrackRemoteIDObservable(Track track, String remoteID);

    Observable<Track> fetchTracks(Observable<List<Track>> track, final boolean lazy);

    Observable<Track> fetchTrack(Observable<Track> track, final boolean lazy);

    Observable<Track> getActiveTrackObservable(boolean lazy);

    void updateTrackMetadata(final Track track, final TrackMetadata trackMetadata) throws
            TrackSerializationException;

    Observable<TrackMetadata> updateTrackMetadataObservable(final Track track, final TrackMetadata trackMetadata) throws
            TrackSerializationException;
}
