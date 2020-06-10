package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BaseLocalCarDAO<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<T> t);

    @Update
    void update(T t);

    @Delete
    void delete(T t);
}
