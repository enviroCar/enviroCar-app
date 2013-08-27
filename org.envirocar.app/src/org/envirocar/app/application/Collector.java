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

import org.envirocar.app.model.MeasurementCandidate;

import android.location.Location;

public class Collector {

	private MeasurementCandidate measurement = new MeasurementCandidate();
	private MeasurementListener callback;
	
	public Collector(MeasurementListener l) {
		this.callback = l;
	}
	
	public void newLocation(Location l) {
		this.measurement.setLocation(l);
		checkStateAndPush();
	}
	
	public void newSpeed(int s) {
		this.measurement.setSpeedMeasurement(s);
		checkStateAndPush();
	}
	
	public void newCO2(double c) {
		this.measurement.setCo2Measurement(c);
		checkStateAndPush();
	}
	
	public void newMAF(double m) {
		this.measurement.setMafMeasurement(m);
		checkStateAndPush();
	}
	
	public void newCalculatedMAF(double m) {
		this.measurement.setCalculatedMafMeasurement(m);
		checkStateAndPush();
	}
	
	public void newRPM(int r) {
		this.measurement.setRpmMeasurement(r);
		checkStateAndPush();
	}
	
	public void newIntakeTemperature(int i) {
		this.measurement.setIntakeTemperatureMeasurement(i);
		checkStateAndPush();
	}
	
	public void newIntakePressure(int p) {
		this.measurement.setIntakePressureMeasurement(p);
		checkStateAndPush();
	}
	
	private void checkStateAndPush() {
		if (measurement == null) return;
		
		if (measurement.ready()) {
			measurement.setResultTime(System.currentTimeMillis());
			insertMeasurement(measurement);
			measurement = new MeasurementCandidate();
		}
	}
	
	
	private void insertMeasurement(MeasurementCandidate m) {
		callback.insertMeasurement(m);
	}


}
