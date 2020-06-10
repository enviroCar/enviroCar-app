package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.Manufacturers;

import java.util.List;

import io.reactivex.Single;


@Dao
public interface LocalManufacturersDAO extends BaseLocalCarDAO<Manufacturers> {

    @Query("SELECT * FROM manufacturers")
    Single<List<Manufacturers>> getAllManufacturers();
}
