package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.envirocar.core.entity.MeasurementTable;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackTable;

import java.sql.Blob;
import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface LocalTrackDAONew {

    @Query("SELECT *FROM TRACKS WHERE _id = :trackId")
    Flowable<TrackTable> getTrack(int trackId);

    @Query("SELECT * FROM TRACKS")
    Flowable<List<TrackTable>> getAllTracks();

    @Query("SELECT * FROM TRACKS WHERE carId =:carId")
    Flowable<List<TrackTable>> getAllTracksByCar(Blob carId);

    @Query("SELECT * FROM TRACKS WHERE remoteId IS NULL")
    Flowable<TrackTable> getAllLocalTracks();

    @Query("SELECT COUNT(*) FROM TRACKS WHERE remoteId IS NULL")
    Flowable<Integer> getAllLocalTracksCount();

    @Query("SELECT * FROM TRACKS WHERE remoteId IS NOT NULL")
    Flowable<TrackTable> getAllRemoteTracks();

    @Insert(onConflict = OnConflictStrategy.FAIL)
    long insertTrack(TrackTable trackTable);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    void insertMeasurement(MeasurementTable measurementTable);

    @Update(onConflict = OnConflictStrategy.FAIL)
    int updateTrack(TrackTable trackTable);

    @Query("UPDATE TRACKS SET carId = :newCarId WHERE carId  = :currentId")
    void updateCarId(Blob newCarId, Blob currentId);

    @Query("DELETE FROM TRACKS WHERE _id = :trackId")
    void deleteTrack(int trackId);

    @Query("UPDATE TRACKS SET remoteId = :remoteId WHERE _id = :trackId")
    void updateTrackRemoteId(Blob remoteId, int trackId);

    @Query("DELETE  FROM measurements WHERE track=:trackId ")
    void deleteMeasuremnt(Track.TrackId trackId);

}
