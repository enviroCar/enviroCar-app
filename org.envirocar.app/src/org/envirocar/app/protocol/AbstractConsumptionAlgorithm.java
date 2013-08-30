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
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;

public abstract class AbstractConsumptionAlgorithm {
	
	public abstract double calculateConsumption(Measurement measurement) throws FuelConsumptionException;
	
	public abstract double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException;
	
	/**
	 * An implementation shall calculate the CO2 emission (kg/h) for a fuel consumption value (l/h)
	 * 
	 * @param consumption fuel consumption in l/h
	 * @param type see {@link FuelType}
	 * @return CO2 emission in kg/h
	 * @throws FuelConsumptionException if the fuelType is not supported
	 */
	public static double calculateCO2FromConsumption(double consumption, FuelType type)
			throws FuelConsumptionException {
		if (type == FuelType.GASOLINE) {
			return consumption * 2.35; //kg/h
		}
		else if (type == FuelType.DIESEL) {
			return consumption * 2.65; //kg/h
		}
		else throw new FuelConsumptionException("Unsupported FuelType "+ type);
	}


}
