/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
