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

import org.envirocar.app.commands.O2LambdaProbe;
import org.envirocar.app.commands.O2LambdaProbeCurrent;
import org.envirocar.app.commands.O2LambdaProbeVoltage;
import org.envirocar.app.event.CO2Event;
import org.envirocar.app.event.ConsumptionEvent;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.GpsDOP;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.event.LocationEventListener;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.protocol.algorithm.AbstractCalculatedMAFAlgorithm;
import org.envirocar.app.protocol.algorithm.AbstractConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.BasicConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.app.protocol.algorithm.UnsupportedFuelTypeException;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;

import android.location.Location;

public class Collector implements LocationEventListener{

	private static final Logger logger = Logger.getLogger(Collector.class);
	static final int DEFAULT_SAMPLING_RATE_DELTA = 5000;
	private Measurement measurement;
	private MeasurementListener callback;
	private Car car;
	private AbstractCalculatedMAFAlgorithm mafAlgorithm;
	private AbstractConsumptionAlgorithm consumptionAlgorithm;
	private boolean fuelTypeNotSupportedLogged;
	private long samplingRateDelta = 5000;

	public Collector(MeasurementListener l, Car car, int samplingDelta) {
		this.callback = l;
		this.car = car;
		
		this.samplingRateDelta = samplingDelta;
		
		this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(this.car);
		logger.info("Using MAF Algorithm "+ this.mafAlgorithm.getClass());
		this.consumptionAlgorithm = new BasicConsumptionAlgorithm(this.car);
		logger.info("Using Consumption Algorithm "+ this.consumptionAlgorithm.getClass());
		
		resetMeasurement();
	}

	
	public Collector(MeasurementListener l, Car car) {
		this(l, car, DEFAULT_SAMPLING_RATE_DELTA);
	}
	
	private void resetMeasurement() {
		measurement = new Measurement(0.0, 0.0);		
	}

	public void newLocation(Location l) {
		this.measurement.setLatitude(l.getLatitude());
		this.measurement.setLongitude(l.getLongitude());
		
		if (l.hasAccuracy() && l.getAccuracy() != 0.0f) {
			this.measurement.setProperty(PropertyKey.GPS_ACCURACY, (double) l.getAccuracy());
		}
		if (l.hasBearing()) {
			this.measurement.setProperty(PropertyKey.GPS_BEARING, (double) l.getBearing());
		}
		if (l.hasAltitude()) {
			this.measurement.setProperty(PropertyKey.GPS_ALTITUDE, l.getAltitude());
		}
		if (l.hasSpeed()) {
			this.measurement.setProperty(PropertyKey.GPS_SPEED, meterPerSecondToKilometerPerHour((double) l.getSpeed()));
		}
		
		checkStateAndPush();
	}
	
	private Double meterPerSecondToKilometerPerHour(double speed) {
		return speed * (36.0/10.0);
	}

	public void newSpeed(int s) {
		this.measurement.setProperty(PropertyKey.SPEED, Double.valueOf(s));
//		checkStateAndPush();
	}
	
	public void newMAF(double m) {
		this.measurement.setProperty(PropertyKey.MAF, m);
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
			try {
				this.measurement.setProperty(PropertyKey.CALCULATED_MAF, this.mafAlgorithm.calculateMAF(this.measurement));
				fireConsumptionEvent();
			} catch (MeasurementsException e) {
				logger.warn(e.getMessage(), e);
			}
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
		} catch (UnsupportedFuelTypeException e) {
			if (!fuelTypeNotSupportedLogged) {
				logger.warn(e.getMessage());
				fuelTypeNotSupportedLogged = true;
			}
		}
		
	}

	public void newIntakeTemperature(int i) {
		this.measurement.setProperty(PropertyKey.INTAKE_TEMPERATURE, Double.valueOf(i));
		checkAndCreateCalculatedMAF();
//		checkStateAndPush();
	}
	
	public void newIntakePressure(int p) {
		this.measurement.setProperty(PropertyKey.INTAKE_PRESSURE, Double.valueOf(p));
		checkAndCreateCalculatedMAF();
//		checkStateAndPush();
	}
	
	public void newTPS(int tps) {
		this.measurement.setProperty(PropertyKey.THROTTLE_POSITON, Double.valueOf(tps));
	}
	
	public void newEngineLoad(double load) {
		this.measurement.setProperty(PropertyKey.ENGINE_LOAD, load);
	}

	public void newDop(GpsDOP dop) {
		if (dop.hasPdop()) {
			this.measurement.setProperty(PropertyKey.GPS_PDOP, dop.getPdop());
		}
		
		if (dop.hasHdop()) {
			this.measurement.setProperty(PropertyKey.GPS_HDOP, dop.getHdop());
		}
		
		if (dop.hasVdop()) {
			this.measurement.setProperty(PropertyKey.GPS_VDOP, dop.getVdop());
		}
	}
	
	/**
	 * currently, this method is only called when a location update
	 * was received. as the update rate of the GPS receiver is
	 * lower (1 Hz probably) then the update rate of the OBD adapter
	 * (revised one) this provides smaller time deltas. A previous location
	 * update could be <= 1 second. Following this approach the delta
	 * is the maximum of the OBD adapter update rate. 
	 */
	private synchronized void checkStateAndPush() {
		if (measurement == null) return;
		
		if (checkReady(measurement)) {
			try {
				double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
				double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);
				this.measurement.setProperty(PropertyKey.CONSUMPTION, consumption);
				this.measurement.setProperty(PropertyKey.CO2, co2);
			} catch (FuelConsumptionException e) {
				logger.warn(e.getMessage());
			} catch (UnsupportedFuelTypeException e) {
				if (!fuelTypeNotSupportedLogged) {
					logger.warn(e.getMessage());
					fuelTypeNotSupportedLogged = true;
				}
			}
			
			/*
			 * update the time as the latest values represent
			 * this measurement
			 */
			measurement.setTime(System.currentTimeMillis());
			
			insertMeasurement(measurement);
			resetMeasurement();
		}
	}
	
	
	private boolean checkReady(Measurement m) {
		if (m.getLatitude() == 0.0 || m.getLongitude() == 0.0) return false;
		
		if (System.currentTimeMillis() - m.getTime() < samplingRateDelta) return false;
		
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
		callback.insertMeasurement(m.carbonCopy());
	}

	public void newFuelSystemStatus(boolean loop, int status) {
		this.measurement.setProperty(PropertyKey.FUEL_SYSTEM_LOOP, loop ? 1d : 0d);
		this.measurement.setProperty(PropertyKey.FUEL_SYSTEM_STATUS_CODE, (double) status);
	}

	public void newLambdaProbeValue(O2LambdaProbe command) {
		if (command instanceof O2LambdaProbeVoltage) {
			this.measurement.setProperty(PropertyKey.LAMBDA_VOLTAGE, ((O2LambdaProbeVoltage) command).getVoltage());	
			this.measurement.setProperty(PropertyKey.LAMBDA_VOLTAGE_ER, command.getEquivalenceRatio());
		}
		else if (command instanceof O2LambdaProbeCurrent) {
			this.measurement.setProperty(PropertyKey.LAMBDA_CURRENT, ((O2LambdaProbeCurrent) command).getCurrent());
			this.measurement.setProperty(PropertyKey.LAMBDA_CURRENT_ER, command.getEquivalenceRatio());
		}
	}

	public void newShortTermTrimBank1(Number numberResult) {
		this.measurement.setProperty(PropertyKey.SHORT_TERM_TRIM_1, numberResult.doubleValue());
	}

	public void newLongTermTrimBank1(Number numberResult) {
		this.measurement.setProperty(PropertyKey.LONG_TERM_TRIM_1, numberResult.doubleValue());		
	}


	@Override
	public void receiveEvent(LocationEvent event) {
		newLocation(event.getPayload());
	}

}
