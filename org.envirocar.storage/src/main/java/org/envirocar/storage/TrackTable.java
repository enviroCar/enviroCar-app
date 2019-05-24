/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
class TrackTable {
    private static final Logger LOG = Logger.getLogger(TrackTable.class);

    public static final String TABLE_TRACK = "tracks";
    public static final String KEY_TRACK_ID = "_id";
    public static final String KEY_TRACK_NAME = "name";
    public static final String KEY_TRACK_DESCRIPTION = "descr";
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

    public static final String[] ALL_TRACK_KEYS = new String[]{
            KEY_TRACK_ID,
            KEY_TRACK_NAME,
            KEY_TRACK_DESCRIPTION,
            KEY_REMOTE_ID,
            KEY_TRACK_STATE,
            KEY_TRACK_METADATA,
            KEY_TRACK_CAR_MANUFACTURER,
            KEY_TRACK_CAR_MODEL,
            KEY_TRACK_CAR_FUEL_TYPE,
            KEY_TRACK_CAR_ENGINE_DISPLACEMENT,
            KEY_TRACK_CAR_YEAR,
            KEY_TRACK_CAR_VIN,
            KEY_TRACK_CAR_ID
    };

    protected static final String CREATE =
            "create table " + TABLE_TRACK + " " +
                    "(" + KEY_TRACK_ID + " INTEGER primary key, " +
                    KEY_TRACK_NAME + " BLOB, " +
                    KEY_TRACK_DESCRIPTION + " BLOB, " +
                    KEY_REMOTE_ID + " BLOB, " +
                    KEY_TRACK_STATE + " BLOB, " +
                    KEY_TRACK_METADATA + " BLOB, " +
                    KEY_TRACK_CAR_MANUFACTURER + " BLOB, " +
                    KEY_TRACK_CAR_MODEL + " BLOB, " +
                    KEY_TRACK_CAR_FUEL_TYPE + " BLOB, " +
                    KEY_TRACK_CAR_ENGINE_DISPLACEMENT + " BLOB, " +
                    KEY_TRACK_CAR_YEAR + " BLOB, " +
                    KEY_TRACK_CAR_VIN + " BLOB, " +
                    KEY_TRACK_CAR_ID + " BLOB);";

    protected static final String DELETE = "DROP TABLE IF EXISTS " + TABLE_TRACK;

    protected static final Func1<Cursor, Track> MAPPER = new Func1<Cursor, Track>() {
        @Override
        public Track call(Cursor cursor) {
            return fromCursor(cursor);
        }
    };

    public static final Func1<? super Cursor, ? extends Observable<Track.TrackId>>
            TO_TRACK_ID_MAPPER = new Func1<Cursor, Observable<Track.TrackId>>() {
        @Override
        public Observable<Track.TrackId> call(Cursor cursor) {
            return Observable.create(new Observable.OnSubscribe<Track.TrackId>() {
                @Override
                public void call(Subscriber<? super Track.TrackId> subscriber) {
                    subscriber.onStart();
                    cursor.moveToFirst();
                    for (int i = 1; cursor.moveToNext() && !subscriber.isUnsubscribed(); i++) {
                        subscriber.onNext(new Track.TrackId(cursor.getLong(
                                cursor.getColumnIndex(KEY_TRACK_ID))));
                    }
                    subscriber.onCompleted();
                }
            });
        }
    };

    public static final Func1<? super Cursor, ? extends List<Track.TrackId>>
            TO_TRACK_ID_LIST_MAPPER = new Func1<Cursor, List<Track.TrackId>>() {
        @Override
        public List<Track.TrackId> call(Cursor cursor) {
            List<Track.TrackId> idList = new ArrayList<>(cursor.getCount());

            while(cursor.moveToNext()){
                idList.add(new Track.TrackId(cursor.getLong(
                        cursor.getColumnIndex(KEY_TRACK_ID))));
            }
            return idList;
        }
    };

    public static ContentValues toContentValues(Track track) {
        ContentValues values = new ContentValues();
        if (track.getTrackID() != null && track.getTrackID().getId() != 0) {
            values.put(KEY_TRACK_ID, track.getTrackID().getId());
        }
        values.put(KEY_TRACK_NAME, track.getName());
        values.put(KEY_TRACK_DESCRIPTION, track.getDescription());
        if (track.isRemoteTrack()) {
            values.put(KEY_REMOTE_ID, track.getRemoteID());
        }
        values.put(KEY_TRACK_STATE, track.getTrackStatus().toString());
        if (track.getCar() != null) {
            values.put(KEY_TRACK_CAR_MANUFACTURER, track.getCar().getManufacturer());
            values.put(KEY_TRACK_CAR_MODEL, track.getCar().getModel());
            values.put(KEY_TRACK_CAR_FUEL_TYPE, track.getCar().getFuelType().name());
            values.put(KEY_TRACK_CAR_ID, track.getCar().getId());
            values.put(KEY_TRACK_CAR_ENGINE_DISPLACEMENT, track.getCar()
                    .getEngineDisplacement());
            values.put(KEY_TRACK_CAR_YEAR, track.getCar().getConstructionYear());
        }

        if (track.getMetadata() != null) {
            try {
                values.put(KEY_TRACK_METADATA, track.getMetadata().toJsonString());
            } catch (JSONException e) {
                LOG.warn(e.getMessage(), e);
            }
        }

        return values;
    }

    public static Track fromCursor(Cursor c) {
        Track track = new TrackImpl();
        track.setTrackID(new Track.TrackId(c.getLong(c.getColumnIndex(KEY_TRACK_ID))));
        track.setRemoteID(c.getString(c.getColumnIndex(KEY_REMOTE_ID)));
        track.setName(c.getString(c.getColumnIndex(KEY_TRACK_NAME)));
        track.setDescription(c.getString(c.getColumnIndex(KEY_TRACK_DESCRIPTION)));

        int statusColumn = c.getColumnIndex(KEY_TRACK_STATE);
        if (statusColumn != -1) {
            track.setTrackStatus(Track.TrackStatus.valueOf(c.getString(statusColumn)));
        } else {
            track.setTrackStatus(Track.TrackStatus.FINISHED);
        }

        String metadata = c.getString(c.getColumnIndex(KEY_TRACK_METADATA));
        if (metadata != null) {
            try {
                track.setMetadata(TrackMetadata.fromJson(c.getString(c.getColumnIndex
                        (KEY_TRACK_METADATA))));
            } catch (JSONException e) {
                LOG.warn(e.getMessage(), e);
            }
        }

        track.setCar(createCarFromCursor(c));

        return track;
    }

    private static Car createCarFromCursor(Cursor c) {
        Log.e("tag", "" + c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER)) + " "
                + c
                .getString(c.getColumnIndex(KEY_TRACK_CAR_ID)));
        if (c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL)) == null ||
                //                c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_YEAR)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_FUEL_TYPE)) == null ||
                c.getString(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT)) == null) {
            return null;
        }

        String manufacturer = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER));
        String model = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL));
        String carId = c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID));
        Car.FuelType fuelType = Car.FuelType.valueOf(c.getString(c.getColumnIndex
                (KEY_TRACK_CAR_FUEL_TYPE)));
        int engineDisplacement = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT));
        int year = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_YEAR));

        return new CarImpl(carId, manufacturer, model, fuelType, year, engineDisplacement);
    }

}
