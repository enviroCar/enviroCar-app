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
package org.envirocar.app.protocol.algorithm;

import static org.envirocar.app.storage.Measurement.PropertyKey.CALCULATED_MAF;
import static org.envirocar.app.storage.Measurement.PropertyKey.MAF;
import static org.envirocar.app.storage.Measurement.PropertyKey.SPEED;
import static org.envirocar.app.storage.Measurement.PropertyKey.LAMBDA_VOLTAGE;

import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;

public class BasicConsumptionAlgorithm extends AbstractConsumptionAlgorithm {

	//minimum required air for diesel
	static final double MRAD = 14.5;
	//diesel fuel density 
	static final double  DFD = 0.832;
	
	//Coefficients to convert O2 lambda voltage to  O2 lambda equivalence ratio default: BMW 525d coefficeients
	private double O2_LAMDA_COEFFICIENT_A = 1.6889627;
	private double O2_LAMDA_COEFFICIENT_B = 0.2560563;
	private double O2_LAMDA_COEFFICIENT_C = 1.0195218;

	private Car car;

	public BasicConsumptionAlgorithm(Car car) {
		this.car = car;
	}

	@Override
	public double calculateConsumption(Measurement measurement) throws FuelConsumptionException, UnsupportedFuelTypeException {

		double maf;
		if (measurement.hasProperty(MAF)) {
			maf = measurement.getProperty(MAF);	
		} else if (measurement.hasProperty(CALCULATED_MAF)) {
			maf = measurement.getProperty(CALCULATED_MAF);
		} else throw new FuelConsumptionException("Get no MAF value");
		
		double consumption;

		if (this.car.getFuelType() == FuelType.GASOLINE) {
			double airFuelRatio = 14.7;
			double fuelDensity = 745;

			//convert from seconds to hour
			 consumption = (maf / airFuelRatio) / fuelDensity * 3600;
		}
		else if (this.car.getFuelType() == FuelType.DIESEL) {

			consumption = caclulateDieselFuelConsumtion(measurement, maf);
		}
		else throw new FuelConsumptionException("FuelType not supported: "+this.car.getFuelType());
		
		return consumption;
	}

	@Override
	public double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException {
		return calculateCO2FromConsumption(consumption, this.car.getFuelType());
	}

	/**
	 * calculates fuel consumption for diesel fuel based on 
	 * https://wiki.52north.org/bin/view/Projects/DieselConsumptionCalculation
	 * 
	 * @param	measurement		mesurements
	 * @param	maf				mass air flow
	 * @return 					diesel fuel comsumption (unit??)
	 */
	private double caclulateDieselFuelConsumtion(Measurement measurement, double maf) throws FuelConsumptionException{
			
			double speed, o2LamdaVoltage;
			
			if (measurement.hasProperty(SPEED)) {
				speed = measurement.getProperty(SPEED);	
			} else throw new FuelConsumptionException("Get no SPEED value");
			
			if (measurement.hasProperty(LAMBDA_VOLTAGE)) {
				o2LamdaVoltage = measurement.getProperty(LAMBDA_VOLTAGE);	
			} else throw new FuelConsumptionException("Get no o2LamdaVoltage value");
		
			//calculating o2 lamda ratio
			o2LamdaRatio = oxygenLamdaRatio(o2LamdaVoltage);
			
			//mass fuel flow 
			double massFuelFlow =  (massAirFlow * 3600) / (o2LamdaRatio * MRAD);
			
			//volumetric fuel flow 
			double volumetricFuelFlow = massFuelFlow * DFD;
			
			return  volumetricFuelFlow * 100 / speed;
	}
	
	/**
	 * The Coefficients are unique to each engine model and are required to calculate O2 lambda EQ from O2 lambda voltage.
	 * It is required due to O2 lambda EQ  data provided by ODB II being capped at 2.0
	 *
	 *@param	O2_LAMDA_COEFFICIENT_A	O2 LAMDA COEFFICIENT A 
	 *@param 	O2_LAMDA_COEFFICIENT_B 	O2 LAMDA COEFFICIENT B
	 *@param	O2_LAMDA_COEFFICIENT_C 	O2 LAMDA COEFFICIENT C
	 */
	public setCoeficients(double O2_LAMDA_COEFFICIENT_A, double O2_LAMDA_COEFFICIENT_B, double O2_LAMDA_COEFFICIENT_C ){
		this.O2_LAMDA_COEFFICIENT_A = O2_LAMDA_COEFFICIENT_A;
		this.O2_LAMDA_COEFFICIENT_B = O2_LAMDA_COEFFICIENT_B;
		this.O2_LAMDA_COEFFICIENT_C = O2_LAMDA_COEFFICIENT_C;
	}

	/**
	 * calculate O2 Lambda Equivalence Ratio using O2 Lambda Voltage Data
	 * 
	 * @param	o2LamdaVoltage	O2 Lambda Voltage
	 * @return 					O2 Lambda Equivalence Ratio
	 */
	private double oxygenLamdaRatio(double o2LamdaVoltage){
		return O2_LAMDA_COEFFICIENT_A * Math.pow( o2LamdaVoltage, 3 ) + O2_LAMDA_COEFFICIENT_B * Math.pow( o2LamdaVoltage, 2 ) + O2_LAMDA_COEFFICIENT_C * o2LamdaVoltage + 1;
	}
	
}
