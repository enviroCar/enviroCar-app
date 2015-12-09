package org.envirocar.obd.processing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.Timestamped;
import org.envirocar.obd.commands.response.DataResponse;

import rx.Observable;

public interface MeasurementProvider {

    Observable<Measurement> measurements(long samplingRate);

    void consider(DataResponse dr);

    void newPosition(Position pos);

    class Position implements Timestamped {

        private final long timestamp;
        private final double latitude;
        private final double longitude;

        public Position(long timestamp, double latitude, double longitude) {
            this.timestamp = timestamp;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
