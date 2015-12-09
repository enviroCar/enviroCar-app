package org.envirocar.obd.events;

import org.envirocar.core.entity.Measurement;

public class PropertyKeyEvent implements Timestamped {

    private final Measurement.PropertyKey propertyKey;
    private final Number value;
    private final long timestamp;

    public PropertyKeyEvent(Measurement.PropertyKey propertyKey, Number value, long timestamp) {
        this.propertyKey = propertyKey;
        this.value = value;
        this.timestamp = timestamp;
    }

    public Measurement.PropertyKey getPropertyKey() {
        return propertyKey;
    }

    public Number getValue() {
        return value;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
