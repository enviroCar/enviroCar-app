package org.envirocar.storage.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.envirocar.core.entity.MeasurementTable;
import org.envirocar.core.entity.TrackTable;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;

@Dao
public interface LocalTrackDAONew {

    @Query("SELECT * FROM TRACKS WHERE _id = :trackId")
    Observable<TrackTable> getTrack(Long trackId);

    @Query("SELECT * FROM TRACKS")
    List<TrackTable> getAllTracks();

    @Query("SELECT * FROM TRACKS WHERE carId =:carId")
    List<TrackTable> getAllTracksByCar(String carId);

    @Query("SELECT * FROM TRACKS WHERE remoteId IS NULL")
    List<TrackTable> getAllLocalTracks();

    @Query("SELECT COUNT(*) FROM TRACKS WHERE remoteId IS NULL")
    Observable<Integer> getAllLocalTracksCount();

    @Query("SELECT * FROM TRACKS WHERE remoteId IS NOT NULL")
    List<TrackTable> getAllRemoteTracks();

    @Query("SELECT _id FROM tracks WHERE remoteId IS NOT NULL")
    Observable<List<Long>> getAllRemoteTracksId();

    @Insert(onConflict = OnConflictStrategy.FAIL)
    long insertTrack(TrackTable trackTable);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    void insertMeasurement(MeasurementTable measurementTable);

    @Update(onConflict = OnConflictStrategy.FAIL)
    int updateTrack(TrackTable trackTable);

    @Query("UPDATE TRACKS SET carId = :newCarId WHERE carId  = :currentId")
    void updateCarId(String newCarId, String currentId);

    @Query("DELETE FROM TRACKS WHERE _id = :trackId")
    void deleteTrack(Long trackId);

    @Query("UPDATE TRACKS SET remoteId = :remoteId WHERE _id = :trackId")
    void updateTrackRemoteId(String remoteId, Long trackId);

    @Query("DELETE  FROM measurements WHERE track=:trackId ")
    void deleteMeasuremnt(Long trackId);

    @Query("SELECT * FROM tracks" +
            " WHERE state" + "='ONGOING'" +
            " ORDER BY _id DESC LIMIT 1")
    Observable<TrackTable> getActiveTrack();

    @Query("DELETE FROM MEASUREMENTS WHERE track=:trackId AND time>=:time")
    void automaticDeleteMeasurement(String time, Long trackId);

    @Query("SELECT * FROM measurements" +
            " WHERE track = :trackId " +
            "ORDER BY time ASC")
    Observable<List<MeasurementTable>> fetchMeasurement(Long trackId);

    @Query("SELECT * FROM MEASUREMENTS" +
            " WHERE track = :trackId " +
            " ORDER BY time ASC")
    List<MeasurementTable> fetchMeasurementSilent(Long trackId);

    @Query("SELECT * FROM measurements WHERE track=:trackId ORDER BY time ASC LIMIT 1")
    Observable<MeasurementTable> fetchStartTime(Long trackId);

    @Query("SELECT time FROM MEASUREMENTS" +
            " WHERE track = :trackId " +
            " ORDER BY time ASC LIMIT 1")
    Cursor fetchStartTimeSilent(Long trackId);

    @Query("SELECT time FROM MEASUREMENTS" +
            " WHERE track = :trackId " +
            " ORDER BY time DESC LIMIT 1")
    Cursor fetchEndTimeSilent(Long trackId);

    @Query("UPDATE TRACKS SET trackMetadata = :metaData WHERE _id = :trackId")
    void updateTrackMetadata(String metaData, Long trackId);
}
