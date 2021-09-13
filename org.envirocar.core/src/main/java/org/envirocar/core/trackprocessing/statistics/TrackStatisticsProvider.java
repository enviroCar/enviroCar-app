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
package org.envirocar.core.trackprocessing.statistics;

import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TrackStatisticsProvider {

    double getDistanceOfTrack();

    double getFuelConsumptionPerHour() throws FuelConsumptionException;

    double getCO2Average() throws FuelConsumptionException;

    double getLiterPerHundredKm() throws FuelConsumptionException, NoMeasurementsException;

    double getGramsPerKm() throws FuelConsumptionException, NoMeasurementsException,
            UnsupportedFuelTypeException;

}
