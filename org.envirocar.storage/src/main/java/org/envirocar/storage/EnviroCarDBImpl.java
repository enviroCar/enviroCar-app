/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.storage;

import android.database.Cursor;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementTable;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackTable;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;


@Singleton
public class EnviroCarDBImpl implements EnviroCarDB {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBImpl.class);

    protected TrackRoomDatabase trackRoomDatabase;

    /**
     * Constructor.
     *
     * @param trackRoomDatabase the Database instance.
     */
    @Inject
    public EnviroCarDBImpl(TrackRoomDatabase trackRoomDatabase) {
        this.trackRoomDatabase = trackRoomDatabase;
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId) {
        return getTrack(trackId, false);
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId, boolean lazy) {
        return fetchTrackObservable(trackId, lazy);
    }

    @Override
    public Observable<List<Track>> getAllTracks() {
        return getAllTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllTracks(final boolean lazy) {
        return fetchTracksObservable(lazy);
    }

    @Override
    public Observable<List<Track>> getAllTracksByCar(String carID, boolean lazy) {
        return fetchTracksCarObservable(carID, lazy);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks() {
        return getAllLocalTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks(boolean lazy) {
        return fetchTracksLocalObservable(lazy);
    }

    @Override
    public Observable<Integer> getAllLocalTracksCount() {
        return trackRoomDatabase.getTrackDAONew().getAllLocalTracksCount();
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks() {
        return getAllRemoteTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks(boolean lazy) {
        return fetchTracksRemoteObservable(lazy);
    }

    @Override
    public Observable<Void> clearTables() {
        return Observable.create(emitter -> {
        });
    }

    public void insertTrack(final Track track) throws TrackSerializationException {
        LOG.info("insertTrack(): trying to insert a new track");
        try {
            long result = trackRoomDatabase.getTrackDAONew().insertTrack(TrackTable.trackToTrackTable(track));
            Track.TrackId trackId = new Track.TrackId(result);
            track.setTrackID(trackId);
            LOG.info(String.format("insertTrack(): " +
                    "track has been successfully inserted ->[id = %s]", "" + result));

            if (track.getMeasurements().size() > 0) {
                for (Measurement measurement : track.getMeasurements()) {
                    measurement.setTrackId(trackId);
                    trackRoomDatabase.getTrackDAONew().insertMeasurement(MeasurementTable.measurementToMeasurementTable(measurement));
                    insertMeasurement(measurement);
                }
            }
        } catch (Exception e) {
            LOG.info(String.format("insertTrack(): " +
                    "insertion fail ->[id = %s]", "" + e.fillInStackTrace()));
        }
    }

    @Override
    public Observable<Track> insertTrackObservable(final Track track) {
        return Observable.create(emitter -> {
            try {
                insertTrack(track);
                emitter.onNext(track);
            } catch (TrackSerializationException e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public boolean updateTrack(Track track) {
        LOG.info(String.format("updateTrack(%s)", track.getTrackID()));
        TrackTable trackTable =TrackTable.trackToTrackTable(track);
        int update = trackRoomDatabase.getTrackDAONew().updateTrack(trackTable);
        return update != -1;
    }

    @Override
    public Observable<Track> updateTrackObservable(Track track) {
        return Observable.just(track)
                .map((Function<Track, Track>) track1 -> {
                    if (updateTrack(track1)) {
                        LOG.info("Track [%s] has been successfully updated.", track1.getDescription());
                    }
                    return track1;
                });
    }

    @Override
    public boolean updateCarIdOfTracks(String currentId, String newId) {
        trackRoomDatabase.getTrackDAONew().updateCarId(newId, currentId);
        return true;
    }

    @Override
    public void deleteTrack(Track.TrackId trackId) {
        trackRoomDatabase.getTrackDAONew().deleteTrack(Long.parseLong(trackId.toString()));
        deleteMeasurementsOfTrack(trackId);
    }

    @Override
    public void deleteTrack(Track track) {
        deleteTrack(track.getTrackID());
    }

    @Override
    public Observable<Track> deleteTrackObservable(Track track) {
        return Observable.create(emitter -> {
            deleteTrack(track);
            emitter.onNext(track);
            emitter.onComplete();
        });
    }

    @Override
    public Observable<List<Track.TrackId>> deleteAllRemoteTracks() {
        return trackRoomDatabase.getTrackDAONew().getAllRemoteTracksId()
                .map(TrackTable.TO_TRACK_ID_LIST_MAPPER)
                .map(trackIds -> {
                    for (Track.TrackId trackId : trackIds)
                        deleteTrack(trackId);
                    return trackIds;
                });
    }

    @Override
    public void insertMeasurement(final Measurement measurement) throws
            MeasurementSerializationException {
        LOG.info("inserted measurement into track " + measurement.getTrackId());
        trackRoomDatabase.getTrackDAONew().insertMeasurement(MeasurementTable.measurementToMeasurementTable(measurement));
    }

    @Override
    public Observable<Void> insertMeasurementObservable(final Measurement measurement) {
        return Observable.create(emitter -> {
            try {
                insertMeasurement(measurement);
            } catch (MeasurementSerializationException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            } finally {
                emitter.onComplete();
            }
        });
    }

    @Override
    public void updateTrackRemoteID(final Track track, final String remoteID) {
        trackRoomDatabase.getTrackDAONew().updateTrackRemoteId(remoteID, Long.parseLong(track.getTrackID().toString()));
    }

    @Override
    public Observable<Void> updateTrackRemoteIDObservable(final Track track, final String
            remoteID) {
        return Observable.create(emitter -> {
            updateTrackRemoteID(track, remoteID);
            emitter.onComplete();
        });
    }

    public void updateTrackMetadata(final Track track, final TrackMetadata trackMetadata) throws
            TrackSerializationException {
        try {
            trackRoomDatabase.getTrackDAONew().updateTrackMetadata(trackMetadata.toJsonString(), Long.parseLong(track.getTrackID().toString()));
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
            throw new TrackSerializationException(e);
        }
    }

    public Observable<TrackMetadata> updateTrackMetadataObservable(
            final Track track, final TrackMetadata trackMetadata) {
        return Observable.create(emitter -> {
            try {
                updateTrackMetadata(track, trackMetadata);
            } catch (TrackSerializationException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            } finally {
                emitter.onComplete();
            }
        });
    }

    @Override
    public Observable<Track> fetchTracks(
            Observable<List<Track>> tracks, final boolean lazy) {
        return fetchTrack(tracks
                .flatMap(tracks1 -> Observable.fromIterable(tracks1)), lazy);
    }

    @Override
    public Observable<Track> fetchTrack(Observable<Track> trackObservable, final boolean lazy) {
        return trackObservable.flatMap(track -> lazy ? fetchStartTime(track) : fetchMeasurements(track));
    }

    @Override
    public Observable<Track> getActiveTrackObservable(boolean lazy) {
        return fetchActiveTrackObservable(lazy);
    }

    private void deleteMeasurementsOfTrack(Track.TrackId trackId) {
        try {
            trackRoomDatabase.getTrackDAONew().deleteMeasuremnt(Long.parseLong(trackId.toString()));
        } catch (Exception e) {
        }
    }

    @Override
    public void automaticDeleteMeasurements(long time, Track.TrackId trackId) {
        try {
            trackRoomDatabase.getTrackDAONew().automaticDeleteMeasurement(String.valueOf(time), Long.parseLong(trackId.toString()));

        } catch (Exception e) {
        }
    }

    private Observable<Track> fetchMeasurements(final Track track) {
        return trackRoomDatabase.getTrackDAONew().fetchMeasurement(Long.parseLong(track.getTrackID().toString()))
                .map(measurementList -> MeasurementTable.fromMeasurementTableListToMeasurement(measurementList))
                .map(measurements -> {
                    track.setMeasurements(measurements);
                    track.setLazyMeasurements(false);
                    return track;
                });
    }

    private Observable<Track> fetchStartTime(final Track track) {
        return trackRoomDatabase.getTrackDAONew().fetchStartTime(Long.parseLong(track.getTrackID().toString()))
                .map(MeasurementTable.MAPPER)
                .map(measurement -> {
                    track.setStartTime(measurement.getTime());
                    track.setLazyMeasurements(true);
                    return track;
                });
    }

    private Observable<Track> fetchTrackObservable(Track.TrackId trackId, boolean lazy) {
        return trackRoomDatabase.getTrackDAONew().getTrack(Long.parseLong(trackId.toString()))
                .map(TrackTable.MAPPER)
                .take(1)
                .timeout(100, TimeUnit.MILLISECONDS)
                .compose(fetchTrackObservable(lazy));
    }

    private Observable<Track> fetchActiveTrackObservable(boolean lazy) {
        return trackRoomDatabase.getTrackDAONew().getActiveTrack()
                .map(org.envirocar.core.entity.TrackTable.MAPPER)
                .take(1)
                .timeout(100, TimeUnit.MILLISECONDS)
                .compose(fetchTrackObservable(lazy));
    }

    private ObservableTransformer<Track, Track> fetchTrackObservable(final boolean lazy) {
        return trackObservable -> trackObservable.map(track -> {
            if (track == null)
                return null;

            return lazy ? fetchStartEndTimeSilent(track) : fetchMeasurementsSilent(track);
        });
    }

    private Observable<List<Track>> fetchTracksObservable(boolean lazy) {
        Observable<List<Track>> listObservable = Observable.create(emitter -> {
            List<TrackTable> trackTableList = trackRoomDatabase
                    .getTrackDAONew().getAllTracks();

            ArrayList<Track> tracks = new ArrayList<>();
            for (org.envirocar.core.entity.TrackTable trackTable : trackTableList) {
                tracks.add(TrackTable.trackTableToTrack(trackTable));
            }

            emitter.onNext(tracks);
            emitter.onComplete();
        });
        return listObservable.compose(fetchTracks(lazy));
    }

    private Observable<List<Track>> fetchTracksCarObservable(String carId, boolean lazy) {
        Observable<List<Track>> listObservable = Observable.create(emitter -> {
            List<TrackTable> trackTableList = trackRoomDatabase
                    .getTrackDAONew().getAllTracksByCar(carId);

            ArrayList<Track> tracks = new ArrayList<>();
            for (TrackTable trackTable : trackTableList) {
                tracks.add(TrackTable.trackTableToTrack(trackTable));
            }

            emitter.onNext(tracks);
            emitter.onComplete();
        });
        return listObservable.compose(fetchTracks(lazy));
    }

    private Observable<List<Track>> fetchTracksLocalObservable(boolean lazy) {
        Observable<List<Track>> listObservable = Observable.create(emitter -> {
            List<org.envirocar.core.entity.TrackTable> trackTableList = trackRoomDatabase
                    .getTrackDAONew().getAllLocalTracks();

            ArrayList<Track> tracks = new ArrayList<>();
            for (org.envirocar.core.entity.TrackTable trackTable : trackTableList) {
                tracks.add(TrackTable.trackTableToTrack(trackTable));
            }

            emitter.onNext(tracks);
            emitter.onComplete();
        });
        return listObservable.compose(fetchTracks(lazy));
    }

    private Observable<List<Track>> fetchTracksRemoteObservable(boolean lazy) {
        Observable<List<Track>> listObservable = Observable.create(emitter -> {
            List<org.envirocar.core.entity.TrackTable> trackTableList = trackRoomDatabase
                    .getTrackDAONew().getAllRemoteTracks();

            ArrayList<Track> tracks = new ArrayList<>();
            for (org.envirocar.core.entity.TrackTable trackTable : trackTableList) {
                tracks.add(TrackTable.trackTableToTrack(trackTable));
            }

            emitter.onNext(tracks);
            emitter.onComplete();
        });
        return listObservable.compose(fetchTracks(lazy));
    }

    private ObservableTransformer<List<Track>, List<Track>> fetchTracks(boolean lazy) {
        return trackObservable -> trackObservable.map(tracks -> {
            for (Track track : tracks) {
                if (lazy) {
                    fetchStartEndTimeSilent(track);
                } else {
                    fetchMeasurementsSilent(track);
                }
            }
            return tracks;
        });
    }

    private Track fetchMeasurementsSilent(final Track track) {
        track.setMeasurements(MeasurementTable.fromMeasurementTableListToMeasurement(
                trackRoomDatabase.getTrackDAONew().fetchMeasurementSilent(Long.parseLong(track.getTrackID().toString()))
        ));
        track.setLazyMeasurements(false);
        return track;
    }

    private Track fetchStartEndTimeSilent(final Track track) {
        Cursor startTime = trackRoomDatabase.getTrackDAONew().fetchStartTimeSilent(Long.parseLong(track.getTrackID().toString()));

        if (startTime.moveToFirst()) {
            track.setStartTime(
                    startTime.getLong(
                            startTime.getColumnIndex(MeasurementTable.KEY_TIME)));
        }

        Cursor endTime = trackRoomDatabase.getTrackDAONew().fetchEndTimeSilent(Long.parseLong(track.getTrackID().toString()));

        if (endTime.moveToFirst()) {
            track.setEndTime(
                    endTime.getLong(
                            startTime.getColumnIndex(MeasurementTable.KEY_TIME)));
        }

        return track;
    }

}
