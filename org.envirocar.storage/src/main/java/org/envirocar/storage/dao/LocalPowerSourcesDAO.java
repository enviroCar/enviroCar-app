package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.PowerSource;

import java.util.List;

import io.reactivex.Single;


@Dao
public interface LocalPowerSourcesDAO extends BaseLocalCarDAO<PowerSource> {

    @Query("SELECT * FROM power_sources")
    Single<List<PowerSource>> getAllPowerSources();

    @Query("SELECT short_name FROM power_sources WHERE id = :id")
    Single<String> getPowerSource(String id);
}
