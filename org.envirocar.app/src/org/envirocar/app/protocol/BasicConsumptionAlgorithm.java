/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.protocol;

import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;

public class BasicConsumptionAlgorithm extends AbstractConsumptionAlgorithm {

	private Car car;

	public BasicConsumptionAlgorithm(Car car) {
		this.car = car;
	}

	@Override
	public double calculateConsumption(Measurement measurement) throws FuelConsumptionException {
		double maf = measurement.getMaf();
		if (maf == 0.0) {
			maf = measurement.getCalculatedMaf();
		}
		
		double airFuelRatio;
		double anotherConstant; //XXX search for its meaning!
		if (this.car.getFuelType() == FuelType.GASOLINE) {
			airFuelRatio = 14.7;
			anotherConstant = 747;
		}
		else if (this.car.getFuelType() == FuelType.DIESEL) {
			airFuelRatio = 14.5;
			anotherConstant = 832;
		}
		else throw new FuelConsumptionException("FuelType not supported: "+this.car.getFuelType());
		
		//conver from seconds to hour
		double consumption = (maf / airFuelRatio) / anotherConstant * 3600;
		
		return consumption;
	}

	@Override
	public double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException {
		return calculateCO2FromConsumption(consumption, this.car.getFuelType());
	}

}
