package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.PowerSource;

import java.util.List;

import io.reactivex.Observable;

@Dao
public interface LocalPowerSourcesDAO extends BaseLocalCarDAO<PowerSource> {

    @Query("SELECT * FROM power_sources")
    Observable<List<PowerSource>> getAllPowerSources();
}
