package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Blob;
import java.util.function.Function;

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
    String name;

    @ColumnInfo(name = KEY_TRACK_DESCRIPTION)
    String description;

    @ColumnInfo(name = KEY_TRACK_START_TIME)
    String startTime;

    @ColumnInfo(name = KEY_TRACK_END_TIME)
    String endTime;

    @ColumnInfo(name = KEY_TRACK_LENGTH)
    String trackLength;

    @ColumnInfo(name = KEY_REMOTE_ID)
    String remoteId;

    @ColumnInfo(name = KEY_TRACK_STATE)
    String trackState;

    @ColumnInfo(name = KEY_TRACK_CAR_MANUFACTURER)
    String carManufacturer;

    @ColumnInfo(name = KEY_TRACK_CAR_MODEL)
    String carModel;

    @ColumnInfo(name = KEY_TRACK_CAR_FUEL_TYPE)
    String carFuelType;

    @ColumnInfo(name = KEY_TRACK_CAR_YEAR)
    String trackCarYear;

    @ColumnInfo(name = KEY_TRACK_CAR_ENGINE_DISPLACEMENT)
    String carEngineDisplacement;

    @ColumnInfo(name = KEY_TRACK_CAR_VIN)
    String carVin;

    @ColumnInfo(name = KEY_TRACK_CAR_ID)
    String carId;

    @ColumnInfo(name = KEY_TRACK_METADATA)
    String carMetadata;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTrackLength() {
        return trackLength;
    }

    public void setTrackLength(String trackLength) {
        this.trackLength = trackLength;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getTrackState() {
        return trackState;
    }

    public void setTrackState(String trackState) {
        this.trackState = trackState;
    }

    public String getCarManufacturer() {
        return carManufacturer;
    }

    public void setCarManufacturer(String carManufacturer) {
        this.carManufacturer = carManufacturer;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarFuelType() {
        return carFuelType;
    }

    public void setCarFuelType(String carFuelType) {
        this.carFuelType = carFuelType;
    }

    public String getTrackCarYear() {
        return trackCarYear;
    }

    public void setTrackCarYear(String trackCarYear) {
        this.trackCarYear = trackCarYear;
    }

    public String getCarEngineDisplacement() {
        return carEngineDisplacement;
    }

    public void setCarEngineDisplacement(String carEngineDisplacement) {
        this.carEngineDisplacement = carEngineDisplacement;
    }

    public String getCarVin() {
        return carVin;
    }

    public void setCarVin(String carVin) {
        this.carVin = carVin;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarMetadata() {
        return carMetadata;
    }

    public void setCarMetadata(String carMetadata) {
        this.carMetadata = carMetadata;
    }

    public  static final Function<TrackTable,Track> MAPPER = trackTable -> trackTableToTrack(trackTable);

    public static Track trackTableToTrack(TrackTable trackTable) {
        Track track = new TrackImpl();
        track.setTrackID(new Track.TrackId(trackTable.getId()));
        track.setRemoteID(trackTable.getRemoteId());
        track.setName(trackTable.getName());
        track.setDescription(trackTable.getDescription());
        track.setStartTime(Long.parseLong(trackTable.getStartTime()));
        track.setEndTime(Long.parseLong(trackTable.getEndTime()));
        track.setLength(Double.parseDouble(trackTable.getTrackLength()));

        //int statusColumn = trackTable.getTrackState();
        return track;
    }
}
