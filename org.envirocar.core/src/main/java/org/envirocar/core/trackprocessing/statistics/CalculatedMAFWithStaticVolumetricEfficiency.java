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
package org.envirocar.core.trackprocessing.statistics;


import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;

public class CalculatedMAFWithStaticVolumetricEfficiency extends
        AbstractCalculatedMAFAlgorithm {

    private static final double GAS_CONSTANT = 8.3144621;
    private static final double MOLECULAR_MASS_OF_AIR = 28.9644;
    private double volumetricEfficiency = 85.0d;
    private Car car;

    public CalculatedMAFWithStaticVolumetricEfficiency(Car car) {
        this.car = car;
    }

    @Override
    public double calculateMAF(double rpm, double intakeTemperature,
                               double intakePressure) {
        //calculate alternative maf from iat (convert to °K), map, rpm
        double imap = rpm * intakePressure / (intakeTemperature + 273.15d);
        //VE = 85 in most modern cars
        double calculatedMaf = imap / 120.0d * this.volumetricEfficiency / 100.0d * CarImpl
                .ccmToLiter(this.car.getEngineDisplacement()) * MOLECULAR_MASS_OF_AIR / GAS_CONSTANT;

        return calculatedMaf;
    }

}
