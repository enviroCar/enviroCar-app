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

    public static String getKeyTrackId() {
        return KEY_TRACK_ID;
    }

    public static String getKeyTrackName() {
        return KEY_TRACK_NAME;
    }

    public static String getKeyTrackDescription() {
        return KEY_TRACK_DESCRIPTION;
    }

    public static String getKeyTrackStartTime() {
        return KEY_TRACK_START_TIME;
    }

    public static String getKeyTrackEndTime() {
        return KEY_TRACK_END_TIME;
    }

    public static String getKeyTrackLength() {
        return KEY_TRACK_LENGTH;
    }

    public static String getKeyRemoteId() {
        return KEY_REMOTE_ID;
    }

    public static String getKeyTrackState() {
        return KEY_TRACK_STATE;
    }

    public static String getKeyTrackCarManufacturer() {
        return KEY_TRACK_CAR_MANUFACTURER;
    }

    public static String getKeyTrackCarModel() {
        return KEY_TRACK_CAR_MODEL;
    }

    public static String getKeyTrackCarFuelType() {
        return KEY_TRACK_CAR_FUEL_TYPE;
    }

    public static String getKeyTrackCarYear() {
        return KEY_TRACK_CAR_YEAR;
    }

    public static String getKeyTrackCarEngineDisplacement() {
        return KEY_TRACK_CAR_ENGINE_DISPLACEMENT;
    }

    public static String getKeyTrackCarVin() {
        return KEY_TRACK_CAR_VIN;
    }

    public static String getKeyTrackCarId() {
        return KEY_TRACK_CAR_ID;
    }

    public static String getKeyTrackMetadata() {
        return KEY_TRACK_METADATA;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Blob getName() {
        return name;
    }

    public void setName(Blob name) {
        this.name = name;
    }

    public Blob getDescription() {
        return description;
    }

    public void setDescription(Blob description) {
        this.description = description;
    }

    public Blob getStartTime() {
        return startTime;
    }

    public void setStartTime(Blob startTime) {
        this.startTime = startTime;
    }

    public Blob getEndTime() {
        return endTime;
    }

    public void setEndTime(Blob endTime) {
        this.endTime = endTime;
    }

    public Blob getTrackLength() {
        return trackLength;
    }

    public void setTrackLength(Blob trackLength) {
        this.trackLength = trackLength;
    }

    public Blob getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(Blob remoteId) {
        this.remoteId = remoteId;
    }

    public Blob getTrackState() {
        return trackState;
    }

    public void setTrackState(Blob trackState) {
        this.trackState = trackState;
    }

    public Blob getCarManufacturer() {
        return carManufacturer;
    }

    public void setCarManufacturer(Blob carManufacturer) {
        this.carManufacturer = carManufacturer;
    }

    public Blob getCarModel() {
        return carModel;
    }

    public void setCarModel(Blob carModel) {
        this.carModel = carModel;
    }

    public Blob getCarFuelType() {
        return carFuelType;
    }

    public void setCarFuelType(Blob carFuelType) {
        this.carFuelType = carFuelType;
    }

    public Blob getTrackCarYear() {
        return trackCarYear;
    }

    public void setTrackCarYear(Blob trackCarYear) {
        this.trackCarYear = trackCarYear;
    }

    public Blob getCarEngineDisplacement() {
        return carEngineDisplacement;
    }

    public void setCarEngineDisplacement(Blob carEngineDisplacement) {
        this.carEngineDisplacement = carEngineDisplacement;
    }

    public Blob getCarVin() {
        return carVin;
    }

    public void setCarVin(Blob carVin) {
        this.carVin = carVin;
    }

    public Blob getCarId() {
        return carId;
    }

    public void setCarId(Blob carId) {
        this.carId = carId;
    }

    public Blob getCarMetadata() {
        return carMetadata;
    }

    public void setCarMetadata(Blob carMetadata) {
        this.carMetadata = carMetadata;
    }
}
