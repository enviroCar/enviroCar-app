package org.envirocar.app.handler.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;

import java.util.Arrays;
import java.util.List;

public interface DataResponseAlgorithm {

    public double calculate(List<PropertyKeyEvent> pke);

    public Measurement.PropertyKey getPropertyKey();

    static List<DataResponseAlgorithm> fromPropertyType(Measurement.PropertyKey pk) {
        switch (pk) {
            case SPEED:
            case GPS_SPEED:
                return Arrays.asList(
                        new MinAccelerationAlgorithm(),
                        new MaxAccelerationAlgorithm()
                );

            default:
                return null;
        }
    }
}
