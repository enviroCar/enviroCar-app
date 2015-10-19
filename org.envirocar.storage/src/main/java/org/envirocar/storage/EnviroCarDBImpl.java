package org.envirocar.storage;

import android.content.ContentValues;
import android.content.Context;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.json.JSONException;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class EnviroCarDBImpl implements EnviroCarDB {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBImpl.class);

    private BriteDatabase database;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public EnviroCarDBImpl(Context context) {
        this.database = SqlBrite.create()
                .wrapDatabaseHelper(new EnviroCarDBOpenHelper(context));
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId) {
        return getTrack(trackId, false);
    }

    @Override
    public Observable<Track> getTrack(Track.TrackId trackId, boolean lazy) {
        return fetchTrack(database.createQuery(
                TrackTable.TABLE_TRACK,
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_TRACK_ID + "=" + trackId)
                .mapToOne(TrackTable.MAPPER), lazy);
    }

    @Override
    public Observable<List<Track>> getAllTracks() {
        return getAllTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllTracks(final boolean lazy) {
        return fetchTracks(database.createQuery(
                TrackTable.TABLE_TRACK, "SELECT * FROM " + TrackTable.TABLE_TRACK)
                .mapToList(TrackTable.MAPPER), lazy);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks() {
        return getAllLocalTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllLocalTracks(boolean lazy) {
        return fetchTracks(database.createQuery(
                TrackTable.TABLE_TRACK,
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NULL")
                .mapToList(TrackTable.MAPPER), lazy);
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks() {
        return getAllRemoteTracks(false);
    }

    @Override
    public Observable<List<Track>> getAllRemoteTracks(boolean lazy) {
        return fetchTracks(database.createQuery(
                TrackTable.TABLE_TRACK,
                "SELECT * FROM " + TrackTable.TABLE_TRACK +
                        " WHERE " + TrackTable.KEY_REMOTE_ID + " IS NOT NULL")
                .mapToList(TrackTable.MAPPER), lazy);
    }

    @Override
    public Observable<Void> clearTables() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                BriteDatabase.Transaction transaction = database.newTransaction();
                // TODO
            }
        });
    }

    @Override
    public Observable<Void> insertTrack(final Track track) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                database.insert(TrackTable.TABLE_TRACK,
                        TrackTable.toContentValues(track));
                try {
                    if (track.getMeasurements().size() > 0) {
                        for (Measurement measurement : track.getMeasurements()) {
                            database.insert(MeasurementTable.TABLE_NAME,
                                    MeasurementTable.toContentValues(measurement));
                        }
                    }
                } catch (MeasurementSerializationException e) {
                    LOG.error(e.getMessage(), e);
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Void> insertMeasurement(final Measurement measurement) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    database.insert(MeasurementTable.TABLE_NAME,
                            MeasurementTable.toContentValues(measurement));
                } catch (MeasurementSerializationException e) {
                    LOG.error(e.getMessage(), e);
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    public Observable<Void> updateTrackRemoteID(final Track track, final String remoteID) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                ContentValues newValues = new ContentValues();
                newValues.put(TrackTable.KEY_REMOTE_ID, remoteID);

                database.update(TrackTable.TABLE_TRACK, newValues,
                        TrackTable.KEY_TRACK_ID + "=?",
                        new String[]{Long.toString(track.getTrackID().getId())});

                subscriber.onCompleted();
            }
        });
    }

    public Observable<Void> updateTrackMetadata(final Track track, final TrackMetadata
            trackMetadata) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    ContentValues newValues = new ContentValues();
                    newValues.put(TrackTable.KEY_TRACK_METADATA, trackMetadata.toJsonString());

                    database.update(TrackTable.TABLE_TRACK, newValues,
                            TrackTable.KEY_TRACK_ID + "=?",
                            new String[]{Long.toString(track.getTrackID().getId())});

                    subscriber.onCompleted();
                } catch (JSONException e) {
                    LOG.error(e.getMessage(), e);
                    subscriber.onError(e);
                }
            }
        });
    }

    private Track getActiveTrackReference() {
        return null;
    }

    private Observable<List<Track>> fetchTracks(
            Observable<List<Track>> tracks, final boolean lazy) {
        return fetchTrack(tracks
                .flatMap(new Func1<List<Track>, Observable<Track>>() {
                    @Override
                    public Observable<Track> call(List<Track> tracks) {
                        return Observable.from(tracks);
                    }
                }), lazy)
                .toList();
    }

    private Observable<Track> fetchTrack(Observable<Track> track, final boolean lazy) {
        return track
                .flatMap(new Func1<Track, Observable<Track>>() {
                    @Override
                    public Observable<Track> call(Track track) {
                        return lazy ? fetchStartTime(track) : fetchMeasurements(track);
                    }
                });
    }

    private Observable<Track> fetchMeasurements(final Track track) {
        return database.createQuery(
                MeasurementTable.TABLE_NAME,
                "SELECT * FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC")
                .mapToList(MeasurementTable.MAPPER)
                .map(new Func1<List<Measurement>, Track>() {
                    @Override
                    public Track call(List<Measurement> measurements) {
                        track.setMeasurements(measurements);
                        track.setLazyMeasurements(false);
                        return track;
                    }
                });
    }

    private Observable<Track> fetchStartTime(final Track track) {
        return database.createQuery(
                MeasurementTable.TABLE_NAME,
                "SELECT * FROM " + MeasurementTable.TABLE_NAME +
                        " WHERE " + MeasurementTable.KEY_TRACK +
                        "=\"" + track.getTrackID() + "\"" +
                        " ORDER BY " + MeasurementTable.KEY_TIME + " ASC" +
                        " LIMIT 1")
                .mapToOne(MeasurementTable.MAPPER)
                .map(new Func1<Measurement, Track>() {
                    @Override
                    public Track call(Measurement measurement) {
                        track.setStartTime(measurement.getTime());
                        track.setLazyMeasurements(true);
                        return track;
                    }
                });
    }
}
