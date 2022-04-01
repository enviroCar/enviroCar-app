package org.envirocar.app.handler.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;

import java.util.Arrays;
import java.util.List;

public interface DataResponseAlgorithm {

    Double calculate(List<PropertyKeyEvent> pke);

    Measurement.PropertyKey getPropertyKey(Measurement.PropertyKey pk);

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
