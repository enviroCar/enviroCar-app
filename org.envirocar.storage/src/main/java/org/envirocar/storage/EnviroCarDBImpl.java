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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.squareup.sqlbrite3.BriteDatabase;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
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


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class EnviroCarDBImpl implements EnviroCarDB {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBImpl.class);

    protected BriteDatabase briteDatabase;

    /**
     * Constructor.
     *
     * @param briteDatabase the Database instance.
     */
    @Inject
    public EnviroCarDBImpl(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId) {
        return getTrack(trackId, false);
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId, boolean lazy) {
        return fetchTrackObservable(
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_TRACK_ID + "=" + trackId, lazy);
    }

    @Override
    public Observable<List<Track>> getAllTracks() {
        return getAllTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllTracks(final boolean lazy) {
        return fetchTracksObservable(
                "SELECT * FROM " + TrackTable.TABLE_TRACK, lazy);
    }

    @Override
    public Observable<List<Track>> getAllTracksByCar(String carID, boolean lazy) {
        return fetchTracksObservable("SELECT * FROM " + TrackTable.TABLE_TRACK +
                " WHERE " + TrackTable.KEY_TRACK_CAR_ID + "='" + carID + "'", lazy);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks() {
        return getAllLocalTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks(boolean lazy) {
        return fetchTracksObservable(
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NULL", lazy);
    }

    @Override
    public Observable<Integer> getAllLocalTracksCount() {
        return Observable.just(briteDatabase.getReadableDatabase().query("SELECT * FROM " + TrackTable.TABLE_TRACK +
                " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NULL").getCount());
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks() {
        return getAllRemoteTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks(boolean lazy) {
        return fetchTracksObservable(
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NOT NULL", lazy);
    }

    @Override
    public Observable<Void> clearTables() {
        return Observable.create(emitter -> {
            BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
            // TODO
        });
    }

    public void insertTrack(final Track track) throws TrackSerializationException {
        LOG.info("insertTrack(): trying to insert a new track");
        BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            long result = briteDatabase.insert(TrackTable.TABLE_TRACK, SQLiteDatabase.CONFLICT_FAIL,
                    TrackTable.toContentValues(track));
            Track.TrackId trackId = new Track.TrackId(result);
            track.setTrackID(trackId);
            LOG.info(String.format("insertTrack(): " +
                    "track has been successfully inserted ->[id = %s]", "" + result));

            if (track.getMeasurements().size() > 0) {
                for (Measurement measurement : track.getMeasurements()) {
                    measurement.setTrackId(trackId);
                    briteDatabase.insert(MeasurementTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL,
                            MeasurementTable.toContentValues(measurement));
                }
            }

            transaction.markSuccessful();
        } finally {
            transaction.close();
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
        ContentValues trackValues = TrackTable.toContentValues(track);
        int update = briteDatabase.update(TrackTable.TABLE_TRACK, SQLiteDatabase.CONFLICT_FAIL, trackValues, TrackTable.KEY_TRACK_ID + "=" + track.getTrackID());
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
        ContentValues values = new ContentValues();
        values.put(TrackTable.KEY_TRACK_CAR_ID, newId);
        briteDatabase.update(TrackTable.TABLE_TRACK, SQLiteDatabase.CONFLICT_FAIL, values,
                TrackTable.KEY_TRACK_CAR_ID + "=?", currentId);
        return true;
    }

    @Override
    public void deleteTrack(Track.TrackId trackId) {
        briteDatabase.delete(TrackTable.TABLE_TRACK,
                TrackTable.KEY_TRACK_ID + "='" + trackId + "'");
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
        return briteDatabase.createQuery(TrackTable.TABLE_TRACK,
                "SELECT " + TrackTable.KEY_TRACK_ID + ", " + TrackTable.KEY_REMOTE_ID +
                        " FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NOT NULL")
                .map(query -> query.run())
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
        briteDatabase.insert(MeasurementTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL,
                MeasurementTable.toContentValues(measurement));
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
        ContentValues newValues = new ContentValues();
        newValues.put(TrackTable.KEY_REMOTE_ID, remoteID);

        briteDatabase.update(TrackTable.TABLE_TRACK, SQLiteDatabase.CONFLICT_FAIL, newValues,
                TrackTable.KEY_TRACK_ID + "=?",
                Long.toString(track.getTrackID().getId()));
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
            ContentValues newValues = new ContentValues();
            newValues.put(TrackTable.KEY_TRACK_METADATA, trackMetadata.toJsonString());

            briteDatabase.update(TrackTable.TABLE_TRACK, SQLiteDatabase.CONFLICT_FAIL, newValues,
                    TrackTable.KEY_TRACK_ID + "=?",
                    Long.toString(track.getTrackID().getId()));
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
        return fetchTrackObservable(
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_TRACK_STATE + "='" +
                        Track.TrackStatus.ONGOING + "'" +
                        " ORDER BY " + TrackTable.KEY_TRACK_ID + " DESC" +
                        " LIMIT 1", lazy);
    }

    private void deleteMeasurementsOfTrack(Track.TrackId trackId) {
        BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            briteDatabase.delete(MeasurementTable.TABLE_NAME,
                    MeasurementTable.KEY_TRACK + "='" + trackId + "'");
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    @Override
    public void automaticDeleteMeasurements(long time, Track.TrackId trackId) {
        BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            briteDatabase.delete(MeasurementTable.TABLE_NAME,
                    MeasurementTable.KEY_TRACK + "='" + trackId + "' AND " + MeasurementTable.KEY_TIME + " >= " + time);
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    private Observable<Track> fetchMeasurements(final Track track) {
        return briteDatabase.createQuery(
                MeasurementTable.TABLE_NAME,
                "SELECT * FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC")
                .mapToList(MeasurementTable.MAPPER)
                .map(measurements -> {
                    track.setMeasurements(measurements);
                    track.setLazyMeasurements(false);
                    return track;
                });
    }

    private Observable<Track> fetchStartTime(final Track track) {
        return briteDatabase.createQuery(
                MeasurementTable.TABLE_NAME,
                "SELECT * FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC" +
                        " LIMIT 1")
                .mapToOne(MeasurementTable.MAPPER)
                .map(measurement -> {
                    track.setStartTime(measurement.getTime());
                    track.setLazyMeasurements(true);
                    return track;
                });
    }

    private Observable<Track> fetchTrackObservable(String sql, boolean lazy) {
        return briteDatabase
                .createQuery(TrackTable.TABLE_TRACK, sql)
                .mapToOne(TrackTable.MAPPER)
                .take(1)
                .timeout(100, TimeUnit.MILLISECONDS)
                .compose(fetchTrackObservable(lazy));
    }

    private ObservableTransformer<Track, Track> fetchTrackObservable(final boolean lazy) {
        return trackObservable -> trackObservable.map(track -> {
            if (track == null)
                return null;

            // return the track either leither or completly fetched.
            return lazy ? fetchStartEndTimeSilent(track) : fetchMeasurementsSilent(track);
        });
    }

    private Observable<List<Track>> fetchTracksObservable(String sql, boolean lazy) {
        Observable<List<Track>> listObservable = Observable.create(emitter -> {
            Cursor query = briteDatabase.getReadableDatabase().query(sql);

            ArrayList<Track> tracks = new ArrayList<>();
            for (query.moveToFirst(); !query.isAfterLast(); query.moveToNext()) {
                tracks.add(TrackTable.MAPPER.apply(query));
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
        track.setMeasurements(MeasurementTable.fromCursorToList(briteDatabase.query(
                "SELECT * FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC", (String[]) null)));
        track.setLazyMeasurements(false);
        return track;
    }

    private Track fetchStartEndTimeSilent(final Track track) {
        Cursor startTime = briteDatabase.query(
                "SELECT " + MeasurementTable.KEY_TIME +
                        " FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC LIMIT 1");

        if (startTime.moveToFirst()) {
            track.setStartTime(
                    startTime.getLong(
                            startTime.getColumnIndex(MeasurementTable.KEY_TIME)));
        }

        Cursor endTime = briteDatabase.query(
                "SELECT " + MeasurementTable.KEY_TIME +
                        " FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " DESC LIMIT 1");

        if (endTime.moveToFirst()) {
            track.setEndTime(
                    endTime.getLong(
                            startTime.getColumnIndex(MeasurementTable.KEY_TIME)));
        }

        return track;
    }

}
