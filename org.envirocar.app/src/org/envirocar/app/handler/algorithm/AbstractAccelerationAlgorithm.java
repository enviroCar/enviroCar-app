/**
 * Copyright (C) 2013 - 2026 the enviroCar community
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

package org.envirocar.app.handler.algorithm;

public class AbstractAccelerationAlgorithm {

    private static final double CONV_FACTOR = 3.6;

    /**
     * Calculates the acceleration in m/s² from two speed values v1 and v2 at time t1 and t2.
     *
     * @param start Speed at time t1
     * @param end Speed at time t2
     * @param startTime Time t1
     * @param endTime Time t2
     * @return Acceleration in m/s²
     */
    public Double calculateAcceleration(Number start, Number end, long startTime, long endTime) {
        if (start == null || end == null) {
            return null;
        }

        double dV = (end.doubleValue() - start.doubleValue());
        double dT = (endTime -  startTime);
        return ((dV / CONV_FACTOR) / (dT / 1000));
    }
}
