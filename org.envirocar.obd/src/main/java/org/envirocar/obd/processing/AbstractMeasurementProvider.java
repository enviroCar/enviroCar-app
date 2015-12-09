package org.envirocar.obd.processing;

import android.location.Location;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.Timestamped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by matthes on 09.12.15.
 */
public abstract class AbstractMeasurementProvider implements MeasurementProvider {

    private Map<PID, Measurement.PropertyKey> pidPropertyMap = new HashMap<>();
    private List<Position> positionBuffer = new ArrayList<>();

    public AbstractMeasurementProvider() {
        pidPropertyMap.put(PID.SPEED, Measurement.PropertyKey.SPEED);
        pidPropertyMap.put(PID.MAF, Measurement.PropertyKey.MAF);
        pidPropertyMap.put(PID.RPM, Measurement.PropertyKey.RPM);
        pidPropertyMap.put(PID.INTAKE_MAP, Measurement.PropertyKey.INTAKE_PRESSURE);
        pidPropertyMap.put(PID.INTAKE_AIR_TEMP, Measurement.PropertyKey.INTAKE_TEMPERATURE);
        pidPropertyMap.put(PID.FUEL_SYSTEM_STATUS, Measurement.PropertyKey.FUEL_SYSTEM_STATUS_CODE);
        pidPropertyMap.put(PID.TPS, Measurement.PropertyKey.THROTTLE_POSITON);
        pidPropertyMap.put(PID.CALCULATED_ENGINE_LOAD, Measurement.PropertyKey.ENGINE_LOAD);
        pidPropertyMap.put(PID.SHORT_TERM_FUEL_TRIM_BANK_1, Measurement.PropertyKey.SHORT_TERM_TRIM_1);
        pidPropertyMap.put(PID.LONG_TERM_FUEL_TRIM_BANK_1, Measurement.PropertyKey.LONG_TERM_TRIM_1);
    }

    protected Measurement.PropertyKey mapPidToProperty(PID pid) {
        return pidPropertyMap.get(pid);
    }

    @Override
    public synchronized void newLocation(Location mLocation) {
        this.positionBuffer.add(new Position(System.currentTimeMillis(),
                mLocation.getLatitude(), mLocation.getLongitude()));
    }

    public synchronized List<Position> getAndClearPositionBuffer() {
        List<Position> result = Collections.unmodifiableList(positionBuffer);
        positionBuffer.clear();
        return result;
    }

    protected class Position implements Timestamped {

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
