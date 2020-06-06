package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.Manufacturers;

import java.util.List;

import io.reactivex.Observable;

@Dao
public interface LocalManufacturersDAO extends BaseLocalCarDAO<Manufacturers> {

    @Query("SELECT * FROM manufacturers")
    Observable<List<Manufacturers>> getAllManufacturers();
}
