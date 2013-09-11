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

import static org.envirocar.app.storage.Measurement.PropertyKey.CO2;
import static org.envirocar.app.storage.Measurement.PropertyKey.CONSUMPTION;
import static org.envirocar.app.storage.Measurement.PropertyKey.INTAKE_PRESSURE;
import static org.envirocar.app.storage.Measurement.PropertyKey.INTAKE_TEMPERATURE;
import static org.envirocar.app.storage.Measurement.PropertyKey.MAF;
import static org.envirocar.app.storage.Measurement.PropertyKey.SPEED;

import org.envirocar.app.event.CO2Event;
import org.envirocar.app.event.ConsumptionEvent;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.protocol.algorithm.AbstractCalculatedMAFAlgorithm;
import org.envirocar.app.protocol.algorithm.AbstractConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.BasicConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;

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
		this.measurement.setProperty(SPEED, Double.valueOf(s));
//		checkStateAndPush();
	}
	
	public void newMAF(double m) {
		this.measurement.setProperty(MAF, m);
//		checkStateAndPush();
		fireConsumptionEvent();
	}
	
	public void newRPM(int r) {
		this.measurement.setProperty(PropertyKey.RPM, Double.valueOf(r));
		checkAndCreateCalculatedMAF();
//		checkStateAndPush();
	}
	
	/**
	 * method checks if the current measurement has everything available for
	 * calculating the MAF, and then calculates it.
	 */
	private void checkAndCreateCalculatedMAF() {
		if (this.measurement.getProperty(PropertyKey.RPM) != null &&
				this.measurement.getProperty(PropertyKey.INTAKE_PRESSURE) != null &&
				this.measurement.getProperty(PropertyKey.INTAKE_TEMPERATURE) != null) {
			this.measurement.setProperty(PropertyKey.CALCULATED_MAF, this.mafAlgorithm.calculateMAF(this.measurement));
			fireConsumptionEvent();
		}
	}

	private void fireConsumptionEvent() {
		try {
			double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
			double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);
			EventBus.getInstance().fireEvent(new ConsumptionEvent(consumption));
			EventBus.getInstance().fireEvent(new CO2Event(co2));
		} catch (FuelConsumptionException e) {
			logger.warn(e.getMessage());
		}
		
	}

	public void newIntakeTemperature(int i) {
		this.measurement.setProperty(INTAKE_TEMPERATURE, Double.valueOf(i));
		checkAndCreateCalculatedMAF();
//		checkStateAndPush();
	}
	
	public void newIntakePressure(int p) {
		this.measurement.setProperty(INTAKE_PRESSURE, Double.valueOf(p));
		checkAndCreateCalculatedMAF();
//		checkStateAndPush();
	}
	
	/**
	 * currently, this method is only called when a location update
	 * was received. as the update rate of the GPS receiver is
	 * lower (1 Hz probably) then the update rate of the OBD adapter
	 * (revised one) this provides smaller time deltas. A previous location
	 * update could be <= 1 second. Following this approach the delta
	 * is the maximum of the OBD adapter update rate. 
	 */
	private void checkStateAndPush() {
		if (measurement == null) return;
		
		if (checkReady(measurement)) {
			try {
				double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
				double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);
				this.measurement.setProperty(CONSUMPTION, consumption);
				this.measurement.setProperty(CO2, co2);
			} catch (FuelConsumptionException e) {
				logger.warn(e.getMessage());
			}
			
			insertMeasurement(measurement);
			resetMeasurement();
		}
	}
	
	
	private boolean checkReady(Measurement m) {
		if (m.getLatitude() == 0.0 || m.getLongitude() == 0.0) return false;
		
		if (System.currentTimeMillis() - m.getTime() < 5000) return false;
		
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
