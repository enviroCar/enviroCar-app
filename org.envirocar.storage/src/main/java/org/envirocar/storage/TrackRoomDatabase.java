package org.envirocar.storage;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.envirocar.core.entity.MeasurementTable;
import org.envirocar.core.entity.TrackTable;
import org.envirocar.storage.dao.LocalTrackDAONew;

@Database(entities = {TrackTable.class, MeasurementTable.class}, version = 1)
public abstract class TrackRoomDatabase extends RoomDatabase {

    public abstract LocalTrackDAONew getTrackDAONew();
}
