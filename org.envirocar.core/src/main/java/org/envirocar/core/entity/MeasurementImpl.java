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
package org.envirocar.core.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class MeasurementImpl implements Measurement {
    protected Track.TrackId trackId;
    protected Double latitude;
    protected Double longitude;
    protected long time;
    protected Map<Measurement.PropertyKey, Double> propertyMap = new HashMap<>();

    /**
     * Constructor.
     */
    public MeasurementImpl() {

    }

    /**
     * Constructor.
     *
     * @param latitude  the latitude value
     * @param longitude the longitude value
     */
    public MeasurementImpl(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public Track.TrackId getTrackId() {
        return trackId;
    }

    @Override
    public void setTrackId(Track.TrackId trackId) {
        this.trackId = trackId;
    }

    @Override
    public Double getLatitude() {
        return latitude;
    }

    @Override
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public Double getLongitude() {
        return longitude;
    }

    @Override
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public Double getProperty(PropertyKey key) {
        return propertyMap.get(key);
    }

    @Override
    public void setProperty(PropertyKey key, Double value) {
        if (value != null) {
            propertyMap.put(key, value);
        }
    }

    @Override
    public boolean hasProperty(PropertyKey key) {
        return propertyMap.containsKey(key);
    }

    @Override
    public Map<PropertyKey, Double> getAllProperties() {
        return propertyMap;
    }

    @Override
    public void setAllProperties(Map<PropertyKey, Double> properties) {
        this.propertyMap = properties;
    }

    @Override
    public Measurement carbonCopy() {
        Measurement res = new MeasurementImpl();
        res.setLongitude(longitude);
        res.setLatitude(latitude);

        for (Map.Entry<PropertyKey, Double> entry : propertyMap.entrySet()) {
            res.setProperty(entry.getKey(), entry.getValue());
        }

        res.setTrackId(trackId);
        res.setTime(time);
        return res;
    }

    @Override
    public void reset() {
        latitude = null;
        longitude = null;

        synchronized (this) {
            propertyMap.clear();
        }
    }
}
