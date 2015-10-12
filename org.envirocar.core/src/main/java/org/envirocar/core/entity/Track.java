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

    Long getStartTime() throws NoMeasurementsException;

    void setStartTime(long startTime);

    Long getEndTime() throws NoMeasurementsException;

    void setEndTime(long endTime);

    long getDuration() throws NoMeasurementsException;

    TrackStatus getTrackStatus();

    void setTrackStatus(TrackStatus trackStatus);

    boolean isFinished();

    TrackMetadata getMetadata();

    void setMetadata(TrackMetadata metadata);

    void updateMetadata(TrackMetadata metadata);

    Measurement getFirstMeasurement() throws NoMeasurementsException;

    Measurement getLastMeasurement() throws NoMeasurementsException;

    List<Measurement> getMeasurements();

    void setMeasurements(List<Measurement> measurements);

    String getRemoteID();

    void setRemoteID(String remoteID);

    boolean isLazyLoadingMeasurements();

    void setLazyLoadingMeasurements(boolean lazyLoadingMeasurements);

    Track carbonCopy();

    boolean isDownloaded();

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
