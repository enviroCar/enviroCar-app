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
