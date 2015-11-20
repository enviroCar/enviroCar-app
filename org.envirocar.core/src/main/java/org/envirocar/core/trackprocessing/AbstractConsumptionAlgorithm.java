/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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


import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public abstract class AbstractConsumptionAlgorithm {

    /**
     * An implementation shall calculate the CO2 emission (kg/h) for a fuel consumption value (l/h)
     *
     * @param consumption fuel consumption in l/h
     * @param type        see {@link Car.FuelType}
     * @return CO2 emission in kg/h
     * @throws FuelConsumptionException if the fuelType is not supported
     */
    public static double calculateCO2FromConsumption(double consumption, Car.FuelType type)
            throws FuelConsumptionException {
        if (type == Car.FuelType.GASOLINE) {
            return consumption * 2.35; //kg/h
        } else if (type == Car.FuelType.DIESEL) {
            return consumption * 2.65; //kg/h
        } else throw new FuelConsumptionException("Unsupported FuelType " + type);
    }

    public abstract double calculateConsumption(Measurement measurement) throws
            FuelConsumptionException, UnsupportedFuelTypeException;

    public abstract double calculateCO2FromConsumption(double consumption) throws
			FuelConsumptionException;
}
