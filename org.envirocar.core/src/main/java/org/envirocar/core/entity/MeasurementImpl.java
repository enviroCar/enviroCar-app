package org.envirocar.core.entity;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class MeasurementImpl implements Measurement {
    protected Track.TrackId trackId;
    protected double latitude;
    protected double longitude;
    protected long time;
    protected Map<Measurement.PropertyKey, Double> propertyMap = Maps.newHashMap();

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
    public double getLatitude() {
        return latitude;
    }

    @Override
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public double getLongitude() {
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
        propertyMap.put(key, value);
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
        latitude = 0;
        longitude = 0;

        synchronized (this) {
            propertyMap.clear();
        }
    }
}
