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

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
class MeasurementTable {
    private static final Logger LOG = Logger.getLogger(MeasurementTable.class);

    public static final String TABLE_NAME = "measurements";
    public static final String KEY_TIME = "time";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_TRACK = "track";

    protected static final String CREATE =
            "create table " + TABLE_NAME + " (" +
                    KEY_ROWID + " INTEGER primary key autoincrement, " +
                    KEY_LATITUDE + " BLOB, " +
                    KEY_LONGITUDE + " BLOB, " +
                    KEY_TIME + " BLOB, " +
                    KEY_PROPERTIES + " BLOB, " +
                    KEY_TRACK + " INTEGER);";

    protected static final String DELETE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    protected static final Func1<Cursor, Measurement> MAPPER = new Func1<Cursor, Measurement>() {
        @Override
        public Measurement call(Cursor cursor) {
            return fromCursor(cursor);
        }
    };

    public static ContentValues toContentValues(Measurement measurement) {
        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, measurement.getLatitude());
        values.put(KEY_LONGITUDE, measurement.getLongitude());
        values.put(KEY_TIME, measurement.getTime());
        values.put(KEY_TRACK, measurement.getTrackId().getId());
        values.put(KEY_PROPERTIES, createPropertiesString(measurement));
        return values;
    }

    private static String createPropertiesString(Measurement measurement) {
        JSONObject result = new JSONObject();
        Map<Measurement.PropertyKey, Double> properties = measurement.getAllProperties();
        for (Measurement.PropertyKey key : properties.keySet()) {
            try {
                result.put(key.name(), properties.get(key));
            } catch (JSONException e) {
                LOG.warn("Error while parsing measurement property "+key.name() +"="+properties.get(key) + "; "+e.getMessage());
            }
        }
        return result.toString();
    }

    public static List<Measurement> fromCursorToList(Cursor c) {
        List<Measurement> res = new ArrayList<>();

        c.moveToFirst();
        for (int i = 1; c.moveToNext(); i++) {
            res.add(fromCursor(c));
        }

        return res;
    }

    public static Measurement fromCursor(Cursor c) {
        Measurement measurement = new MeasurementImpl();
        measurement.setLatitude(c.getDouble(c.getColumnIndex(KEY_LATITUDE)));
        measurement.setLongitude(c.getDouble(c.getColumnIndex(KEY_LONGITUDE)));
        measurement.setTime(c.getLong(c.getColumnIndex(KEY_TIME)));
        measurement.setTrackId(new Track.TrackId(
                c.getLong(c.getColumnIndex(KEY_TRACK))));

        String rawData = c.getString(c.getColumnIndex(KEY_PROPERTIES));
        if (rawData != null) {
            try {
                JSONObject json = new JSONObject(rawData);
                JSONArray names = json.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String key = names.getString(j);
                        measurement.setProperty(Measurement.PropertyKey.valueOf(key), json
                                .getDouble(key));
                    }
                }
            } catch (JSONException e) {
                LOG.severe("could not load properties", e);
            }
        }
        return measurement;
    }
}
