package org.envirocar.app.handler.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;

import java.util.List;

public class MaxAccelerationAlgorithm implements DataResponseAlgorithm {

    @Override
    public double calculate(List<PropertyKeyEvent> pke) {
        return Math.random() * 5;
    }

    @Override
    public Measurement.PropertyKey getPropertyKey() {
        return Measurement.PropertyKey.MAX_ACCELERATION;
    }
}
