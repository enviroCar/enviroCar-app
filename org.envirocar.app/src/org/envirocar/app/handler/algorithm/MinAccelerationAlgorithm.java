package org.envirocar.app.handler.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;

import java.util.List;

public class MinAccelerationAlgorithm extends AbstractAccelerationAlgorithm implements DataResponseAlgorithm{

    @Override
    public Double calculate(List<PropertyKeyEvent> pkes) {
        if(pkes.size() < 2) {
            return null;
        }
        double minAcc = calculateAcceleration(pkes.get(0).getValue(), pkes.get(1).getValue(),
                pkes.get(0).getTimestamp(), pkes.get(1).getTimestamp());

        for(int i = 1; i < pkes.size() - 1; i++) {
            Double acc = calculateAcceleration(pkes.get(i).getValue(), pkes.get(i + 1).getValue(),
                    pkes.get(i).getTimestamp(), pkes.get(i + 1).getTimestamp());
            if(acc != null && acc < minAcc){
                minAcc = acc;
            }
        }

        return minAcc;
    }

    @Override
    public Measurement.PropertyKey getPropertyKey() {
        return Measurement.PropertyKey.MIN_ACCELERATION;
    }
}
