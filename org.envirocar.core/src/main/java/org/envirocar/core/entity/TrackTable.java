/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.entity;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.functions.Function;

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

    private static final Logger LOG = Logger.getLogger(TrackTable.class);

    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = KEY_TRACK_ID)
    Long id;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public static final Function<? super TrackTable, ? extends Track> MAPPER = trackTable -> trackTableToTrack(trackTable);

    public static Track trackTableToTrack(TrackTable trackTable) {
        Track track = new TrackImpl();
        track.setTrackID(new Track.TrackId(trackTable.getId()));
        track.setRemoteID(trackTable.getRemoteId());
        track.setName(trackTable.getName());
        track.setDescription(trackTable.getDescription());
        track.setStartTime(Long.parseLong(trackTable.getStartTime()));
        track.setEndTime(Long.parseLong(trackTable.getEndTime()));
        track.setLength(Double.parseDouble(trackTable.getTrackLength()));

        if (trackTable.getTrackState() != null) {
            track.setTrackStatus(Track.TrackStatus.valueOf(trackTable.getTrackState()));
        } else {
            track.setTrackStatus(Track.TrackStatus.FINISHED);
        }

        if (trackTable.getCarMetadata() != null) {
            try {
                track.setMetadata(TrackMetadata.fromJson(trackTable.getCarMetadata()));
            } catch (JSONException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        track.setCar(createCarFromTrackTable(trackTable));

        return track;
    }

    private static Car createCarFromTrackTable(TrackTable trackTable) {
        if (trackTable.getCarManufacturer() == null ||
                trackTable.getCarModel() == null ||
                trackTable.getTrackCarYear() == null ||
                trackTable.getCarFuelType() == null ||
                trackTable.getCarEngineDisplacement() == null) {
            return null;
        }

        String manufacturer = trackTable.getCarManufacturer();
        String model = trackTable.getCarModel();
        String carId = trackTable.getCarId();
        Car.FuelType fuelType = Car.FuelType.valueOf(trackTable.getCarFuelType());
        int engineDisplacement = Integer.parseInt(trackTable.getCarEngineDisplacement());
        int year = Integer.parseInt(trackTable.getTrackCarYear());

        return new CarImpl(carId, manufacturer, model, fuelType, year, engineDisplacement);
    }

    public static TrackTable trackToTrackTable(Track track) {
        TrackTable trackTable = new TrackTable();
        if (track.getTrackID() != null && track.getTrackID().getId() != 0) {
            trackTable.setId(Long.parseLong(track.getTrackID().toString()));
        }
        trackTable.setName(track.getName());
        trackTable.setDescription(track.getDescription());
        if (track.isRemoteTrack()) {
            trackTable.setRemoteId(track.getRemoteID());
        }
        trackTable.setTrackState(track.getTrackStatus().toString());
        trackTable.setStartTime(track.getStartTime().toString());
        if (track.getEndTime() != null)
            trackTable.setEndTime(track.getEndTime().toString());
        trackTable.setTrackLength(track.getLength().toString());
        if (track.getCar() != null) {
            trackTable.setCarManufacturer(track.getCar().getManufacturer());
            trackTable.setCarModel(track.getCar().getModel());
            trackTable.setCarFuelType(track.getCar().getFuelType().name());
            trackTable.setCarId(track.getCar().getId());
            trackTable.setCarEngineDisplacement("" + track.getCar().getEngineDisplacement());
            trackTable.setTrackCarYear("" + track.getCar().getConstructionYear());
        }

        if (track.getMetadata() != null) {
            try {
                trackTable.setCarMetadata(track.getMetadata().toJsonString());
            } catch (JSONException e) {
                LOG.warn(e.getMessage(), e);
            }
        }

        return trackTable;
    }

    public static final Function<? super List<Long>, ? extends List<Track.TrackId>>
            TO_TRACK_ID_LIST_MAPPER = (Function<List<Long>, List<Track.TrackId>>) trackIdList -> {
        List<Track.TrackId> idList = new ArrayList<>(trackIdList.size());

        for (Long trackId : trackIdList) {
            idList.add(new Track.TrackId(trackId));
        }
        return idList;
    };
}
