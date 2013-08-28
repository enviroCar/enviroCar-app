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
package org.envirocar.app.application;

import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.protocol.AbstractCalculatedMAFAlgorithm;
import org.envirocar.app.protocol.AbstractConsumptionAlgorithm;
import org.envirocar.app.protocol.BasicConsumptionAlgorithm;
import org.envirocar.app.protocol.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.app.storage.Measurement;

import android.location.Location;

public class Collector {

	private static final Logger logger = Logger.getLogger(Collector.class);
	private Measurement measurement;
	private MeasurementListener callback;
	private Car car;
	private AbstractCalculatedMAFAlgorithm mafAlgorithm;
	private AbstractConsumptionAlgorithm consumptionAlgorithm;
	
	public Collector(MeasurementListener l, Car car) {
		this.callback = l;
		this.car = car;
		
		this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(this.car);
		logger.info("Using MAF Algorithm "+ this.mafAlgorithm.getClass());
		this.consumptionAlgorithm = new BasicConsumptionAlgorithm(this.car);
		logger.info("Using Consumption Algorithm "+ this.consumptionAlgorithm.getClass());
		
		resetMeasurement();
	}
	
	private void resetMeasurement() {
		measurement = new Measurement(0.0, 0.0);		
	}

	public void newLocation(Location l) {
//		this.measurement.setLocation(l);
		this.measurement.setLatitude(l.getLatitude());
		this.measurement.setLongitude(l.getLongitude());
		checkStateAndPush();
	}
	
	public void newSpeed(int s) {
		this.measurement.setSpeed(s);
		checkStateAndPush();
	}
	
	public void newCO2(double c) {
		this.measurement.setCO2(c);
		checkStateAndPush();
	}
	
	public void newConsumption(double c) {
		this.measurement.setConsumption(c);
		checkStateAndPush();
	}
	
	public void newMAF(double m) {
		this.measurement.setMaf(m);
		checkStateAndPush();
	}
	
	public void newRPM(int r) {
		this.measurement.setRpm(r);
		checkAndCreateCalculatedMAF();
		checkStateAndPush();
	}
	
	/**
	 * method checks if the current measurement has everything available for
	 * calculating the MAF, and then calculates it.
	 */
	private void checkAndCreateCalculatedMAF() {
		if (this.measurement.getRpm() != 0.0 &&
				this.measurement.getIntakePressure() != 0 &&
				this.measurement.getIntakePressure() != 0) {
			this.measurement.setCalculatedMaf(this.mafAlgorithm.calculateMAF(this.measurement));
		}
	}

	public void newIntakeTemperature(int i) {
		this.measurement.setIntakeTemperature(i);
		checkAndCreateCalculatedMAF();
		checkStateAndPush();
	}
	
	public void newIntakePressure(int p) {
		this.measurement.setIntakePressure(p);
		checkAndCreateCalculatedMAF();
		checkStateAndPush();
	}
	
	private void checkStateAndPush() {
		if (measurement == null) return;
		
		if (checkReady(measurement)) {
			try {
				double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
				double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);
				this.measurement.setConsumption(consumption);
				this.measurement.setCO2(co2);
			} catch (FuelConsumptionException e) {
				logger.warn(e.getMessage(), e);
			}
			
			insertMeasurement(measurement);
			resetMeasurement();
		}
	}
	
	
	private boolean checkReady(Measurement m) {
		if (m.getLatitude() == 0.0 || m.getLongitude() == 0.0) return false;
		
		if (System.currentTimeMillis() - m.getMeasurementTime() < 5000) return false;
		
		/*
		 * emulate the legacy behavior: insert measurement despite data might be missing
		 */
//		if (m.getSpeed() == 0) return false;
//		
//		if (m.getCO2() == 0.0) return false;
//		
//		if (m.getConsumption() == 0.0) return false;
//		
//		if (m.getCalculatedMaf() == 0.0 || m.getMaf() == 0.0) return false;
//		
//		if (m.getRpm() == 0) return false;
//		
//		if (m.getIntakePressure() == 0) return false;
//		
//		if (m.getIntakeTemperature() == 0) return false;
		
		return true;
	}

	private void insertMeasurement(Measurement m) {
		callback.insertMeasurement(m);
	}


}
