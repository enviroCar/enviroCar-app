/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.trackprocessing.consumption;


import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface ConsumptionAlgorithm {
    double DIESEL_CONSUMPTION_TO_CO2_FACTOR = 2.65;
    double GASOLINE_CONSUMPTION_TO_CO2_FACTOR = 2.35;

    /**
     * An implementation shall calculate the fuel consumption (l/h).
     *
     * @param measurement the measurement providing the required parameters
     * @return fuel consumption in l/h
     * @throws FuelConsumptionException     if required parameters were missing
     * @throws UnsupportedFuelTypeException if fuel type is not supported
     */
    double calculateConsumption(Measurement measurement) throws
            FuelConsumptionException, UnsupportedFuelTypeException;

    /**
     * An implementation shall calculate the CO2 emission (kg/h) for a fuel consumption value (l/h)
     *
     * @param consumption fuel consumption in l/h
     * @return CO2 emission in kg/h
     * @throws FuelConsumptionException if the fuelType is not supported
     */
    double calculateCO2FromConsumption(double consumption) throws
            FuelConsumptionException;

    /**
     * Resolves the ConsumptionAlgorithm for a specific FuelType.
     *
     * @return the consumption algorithm for a specific FuelType.
     */
    static ConsumptionAlgorithm fromFuelType(Car.FuelType fuelType) {
        switch (fuelType) {
            case DIESEL:
                return new DieselConsumptionAlgorithm();
            case GASOLINE:
            case GAS:
            case HYBRID:
                return new GasolineConsumptionAlgorithm();
            default:
                return null;
        }
    }
}
