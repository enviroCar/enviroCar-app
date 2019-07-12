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

import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.util.TrackMetadata;

import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Track extends BaseEntity<Track>, Comparable<Track> {
    String KEY_TRACK_TYPE = "type";
    String KEY_TRACK_PROPERTIES = "properties";
    String KEY_TRACK_PROPERTIES_ID = "id";
    String KEY_TRACK_PROPERTIES_NAME = "name";
    String KEY_TRACK_PROPERTIES_DESCRIPTION = "description";
    String KEY_TRACK_PROPERTIES_CREATED = "created";
    String KEY_TRACK_PROPERTIES_MODIFIED = "modified";
    String KEY_TRACK_PROPERTIES_SENSOR = "sensor";
    String KEY_TRACK_PROPERTIES_LENGTH = "length";
    String KEY_TRACK_PROPERTIES_BEGIN = "begin";
    String KEY_TRACK_PROPERTIES_END = "end";

    String KEY_TRACK_FEATURES = "features";
    String KEY_TRACK_FEATURES_GEOMETRY = "geometry";
    String KEY_TRACK_FEATURES_GEOMETRY_COORDINATES = "coordinates";
    String KEY_TRACK_FEATURES_PROPERTIES = "properties";
    String KEY_TRACK_FEATURES_PROPERTIES_ID = "id";
    String KEY_TRACK_FEATURES_PROPERTIES_TIME = "time";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS = "phenomenons";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_VALUE = "value";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_UNIT = "unit";

    enum TrackStatus {
        ONGOING {
            @Override
            public String toString() {
                return "ONGOING";
            }

        },

        FINISHED {
            @Override
            public String toString() {
                return "FINISHED";
            }
        }
    }

    enum DownloadState {
        REMOTE,
        DOWNLOADING,
        DOWNLOADED,
    }

    TrackId getTrackID();

    void setTrackID(TrackId trackId);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Car getCar();

    void setCar(Car car);

    boolean isLocalTrack();

    boolean isRemoteTrack();

    Long getLastModified();

    void setLastModified(long lastModified);

    Long getStartTime() throws NoMeasurementsException;

    void setStartTime(Long startTime);

    Long getEndTime() throws NoMeasurementsException;

    void setEndTime(Long endTime);

    long getDuration() throws NoMeasurementsException;

    String getBegin();

    void setBegin(String begin);

    String getEnd();

    void setEnd(String end);

    long getTimeInMillis();

    Float getLength();

    void setLength(Float length);

    TrackStatus getTrackStatus();

    void setTrackStatus(TrackStatus trackStatus);

    boolean isFinished();

    TrackMetadata getMetadata();

    void setMetadata(TrackMetadata metadata);

    TrackMetadata updateMetadata(TrackMetadata metadata);

    Measurement getFirstMeasurement() throws NoMeasurementsException;

    Measurement getLastMeasurement() throws NoMeasurementsException;

    List<Measurement> getMeasurements();

    void setMeasurements(List<Measurement> measurements);

    boolean hasProperty(Measurement.PropertyKey propertyKey);

    List<Measurement.PropertyKey> getSupportedProperties();

    String getRemoteID();

    void setRemoteID(String remoteID);

    boolean isLazyLoadingMeasurements();

    void setLazyMeasurements(boolean lazyLoadingMeasurements);

    Track carbonCopy();

    boolean isDownloaded();

    boolean isDownloading();

    DownloadState getDownloadState();

    void setDownloadState(DownloadState downloadState);

    /**
     * TODO JavaDoc
     */
    class TrackId {
        private long id;

        public TrackId(long i) {
            this.id = i;
        }

        public long getId() {
            return id;
        }

        @Override
        public String toString() {
            return Long.toString(id);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o instanceof TrackId) {
                return (this.getId() == ((TrackId) o).getId());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) this.id;
        }
    }
}
