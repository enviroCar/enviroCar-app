package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Blob;

@Entity(tableName = "measurements")
public class MeasurementTable {

    public static final String KEY_TIME = "time";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_TRACK = "track";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = KEY_ROWID)
    int rowId;

    @ColumnInfo(name = KEY_LATITUDE)
    Blob keyLatitude;

    @ColumnInfo(name = KEY_LONGITUDE)
    Blob keyLongitude;

    @ColumnInfo(name = KEY_PROPERTIES)
    Blob keyProperties;

    @ColumnInfo(name = KEY_TIME)
    Blob keyTime;

    @ColumnInfo(name = KEY_TRACK)
    int keyTrack;

}
