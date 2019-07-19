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

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class GasolineConsumptionAlgorithm implements ConsumptionAlgorithm {
    private static final double AIR_FUEL_RATIO = 14.7;
    private static final double FUEL_DENSITY = 745;

    /**
     * @param measurement the measurement providing the required parameters
     * @return the estimated fuel consumption for gasoline.
     * @throws FuelConsumptionException
     */
    @Override
    public double calculateConsumption(Measurement measurement) throws FuelConsumptionException {
        double maf;
        if (measurement.hasProperty(Measurement.PropertyKey.MAF)) {
            maf = measurement.getProperty(Measurement.PropertyKey.MAF);
        } else if (measurement.hasProperty(Measurement.PropertyKey.CALCULATED_MAF)) {
            maf = measurement.getProperty(Measurement.PropertyKey.CALCULATED_MAF);
        } else throw new FuelConsumptionException("Get no MAF value");

        //convert from seconds to hour
        double result_in_seconds = (maf / AIR_FUEL_RATIO) / FUEL_DENSITY;
        return result_in_seconds * 3600;
    }

    /**
     * @param consumption fuel consumption in l/h
     * @return the estimated co2 consumption
     */
    @Override
    public double calculateCO2FromConsumption(double consumption) {
        return consumption * GASOLINE_CONSUMPTION_TO_CO2_FACTOR; //kg/h
    }
}
