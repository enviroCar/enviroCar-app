package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Blob;

@Entity(tableName = "tracks")
public class TrackTable {

    public static final String KEY_TRACK_ID = "_id";
    public static final String KEY_TRACK_NAME = "name";
    public static final String KEY_TRACK_DESCRIPTION = "descr";
    public static final String KEY_TRACK_START_TIME = "start_time";
    public static final String KEY_TRACK_END_TIME = "end_time";
    public static final String KEY_TRACK_LENGTH = "length";
    public static final String KEY_REMOTE_ID = "remoteId";
    public static final String KEY_TRACK_STATE = "state";
    public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
    public static final String KEY_TRACK_CAR_MODEL = "car_model";
    public static final String KEY_TRACK_CAR_FUEL_TYPE = "fuel_type";
    public static final String KEY_TRACK_CAR_YEAR = "car_construction_year";
    public static final String KEY_TRACK_CAR_ENGINE_DISPLACEMENT = "engine_displacement";
    public static final String KEY_TRACK_CAR_VIN = "vin";
    public static final String KEY_TRACK_CAR_ID = "carId";
    public static final String KEY_TRACK_METADATA = "trackMetadata";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = KEY_TRACK_ID)
    int id;

    @ColumnInfo(name = KEY_TRACK_NAME)
    Blob name;

    @ColumnInfo(name = KEY_TRACK_DESCRIPTION)
    Blob description;

    @ColumnInfo(name = KEY_TRACK_START_TIME)
    Blob startTime;

    @ColumnInfo(name = KEY_TRACK_END_TIME)
    Blob endTime;

    @ColumnInfo(name = KEY_TRACK_LENGTH)
    Blob trackLength;

    @ColumnInfo(name = KEY_REMOTE_ID)
    Blob remoteId;

    @ColumnInfo(name = KEY_TRACK_STATE)
    Blob trackState;

    @ColumnInfo(name = KEY_TRACK_CAR_MANUFACTURER)
    Blob carManufacturer;

    @ColumnInfo(name = KEY_TRACK_CAR_MODEL)
    Blob carModel;

    @ColumnInfo(name = KEY_TRACK_CAR_FUEL_TYPE)
    Blob carFuelType;

    @ColumnInfo(name = KEY_TRACK_CAR_YEAR)
    Blob trackCarYear;

    @ColumnInfo(name = KEY_TRACK_CAR_ENGINE_DISPLACEMENT)
    Blob carEngineDisplacement;

    @ColumnInfo(name = KEY_TRACK_CAR_VIN)
    Blob carVin;

    @ColumnInfo(name = KEY_TRACK_CAR_ID)
    Blob carId;

    @ColumnInfo(name = KEY_TRACK_METADATA)
    Blob carMetadata;

}
