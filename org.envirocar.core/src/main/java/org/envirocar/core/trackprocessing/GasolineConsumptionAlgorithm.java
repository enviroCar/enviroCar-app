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

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class GasolineConsumptionAlgorithm implements ConsumptionAlgorithm {


    @Override
    public double calculateConsumption(Measurement measurement) throws FuelConsumptionException,
            UnsupportedFuelTypeException {
        double maf;
        if (measurement.hasProperty(Measurement.PropertyKey.MAF)) {
            maf = measurement.getProperty(Measurement.PropertyKey.MAF);
        } else if (measurement.hasProperty(Measurement.PropertyKey.CALCULATED_MAF)) {
            maf = measurement.getProperty(Measurement.PropertyKey.CALCULATED_MAF);
        } else throw new FuelConsumptionException("Get no MAF value");

        double airFuelRatio = 14.7;
        double fuelDensity = 745;

        //convert from seconds to hour
        return (maf / airFuelRatio) / fuelDensity * 3600;
    }

    @Override
    public double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException {
        return consumption * 2.35; //kg/h
    }
}
