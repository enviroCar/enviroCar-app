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
package org.envirocar.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.envirocar.obd.events.Timestamped;

import rx.Observable;

/**
 * TODO JavaDoc
 */
public interface MeasurementProvider {

    Observable<Measurement> measurements(long samplingRate);

    void consider(PropertyKeyEvent pke);

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
