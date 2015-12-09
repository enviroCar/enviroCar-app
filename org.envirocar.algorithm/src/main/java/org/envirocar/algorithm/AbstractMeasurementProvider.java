package org.envirocar.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.PID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMeasurementProvider implements MeasurementProvider {

    private List<Position> positionBuffer = new ArrayList<>();

    @Override
    public synchronized void newPosition(Position pos) {
        this.positionBuffer.add(pos);
    }

    public synchronized List<Position> getAndClearPositionBuffer() {
        List<Position> result = Collections.unmodifiableList(positionBuffer);
        positionBuffer = new ArrayList<>(result.size());
        return result;
    }

}
