package org.envirocar.core.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.Manufacturers;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface CarNewDAO {
    @Query("SELECT * FROM manufacturers")
    Single<List<Manufacturers>> getAll();
}
