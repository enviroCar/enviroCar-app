/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.core.trackprocessing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.NoMeasurementsException;

import static org.envirocar.core.entity.Measurement.PropertyKey.INTAKE_PRESSURE;
import static org.envirocar.core.entity.Measurement.PropertyKey.INTAKE_TEMPERATURE;
import static org.envirocar.core.entity.Measurement.PropertyKey.RPM;


public abstract class AbstractCalculatedMAFAlgorithm {

    public abstract double calculateMAF(double rpm, double intakeTemperature, double
            intakePressure);

    public double calculateMAF(Measurement m) throws NoMeasurementsException {
        if (m == null) {
            throw new NoMeasurementsException("Measurement was null!");
        } else if (m.hasProperty(RPM) && m.hasProperty(INTAKE_TEMPERATURE) && m.hasProperty
				(INTAKE_PRESSURE)) {
            return calculateMAF(m.getProperty(RPM), m.getProperty(INTAKE_TEMPERATURE), m
					.getProperty(INTAKE_PRESSURE));
        }

        throw new NoMeasurementsException("Measurement did not carry all required properties!");
    }

}
