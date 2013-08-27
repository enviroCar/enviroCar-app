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
package org.envirocar.app.model;

import android.location.Location;

public class MeasurementCandidate {
	
	private double calculatedMafMeasurement;
	private double co2Measurement;
	private int intakePressureMeasurement;
	private int intakeTemperatureMeasurement;
	private Location location;
	private double mafMeasurement;
	private long resultTime;
	private int rpmMeasurement;
	private int speedMeasurement;
	
	public boolean ready() {
		if (location == null) return false;
		
		if (speedMeasurement == 0) return false;
		
		if (co2Measurement == 0) return false;
		
		if (getMAF() == 0) return false;
		
		if (rpmMeasurement == 0) return false;
		
		if (intakePressureMeasurement == 0) return false;
		
		if (intakeTemperatureMeasurement == 0) return false;
		
		return true;
	}
	
	public double getCo2Measurement() {
		return co2Measurement;
	}


	public int getIntakePressureMeasurement() {
		return intakePressureMeasurement;
	}


	public int getIntakeTemperatureMeasurement() {
		return intakeTemperatureMeasurement;
	}


	public Location getLocation() {
		return location;
	}


	public double getMAF() {
		if (mafMeasurement != 0) return mafMeasurement;
		return calculatedMafMeasurement;
	}


	public long getResultTime() {
		return resultTime;
	}


	public int getRpmMeasurement() {
		return rpmMeasurement;
	}


	public int getSpeedMeasurement() {
		return speedMeasurement;
	}


	public void setCalculatedMafMeasurement(double calculatedMafMeasurement) {
		this.calculatedMafMeasurement = calculatedMafMeasurement;
	}


	public void setCo2Measurement(double co2Measurement) {
		this.co2Measurement = co2Measurement;
	}


	public void setIntakePressureMeasurement(int intakePressureMeasurement) {
		this.intakePressureMeasurement = intakePressureMeasurement;
	}


	public void setIntakeTemperatureMeasurement(int intakeTemperatureMeasurement) {
		this.intakeTemperatureMeasurement = intakeTemperatureMeasurement;
	}


	public void setLocation(Location location) {
		this.location = location;
	}


	public void setMafMeasurement(double mafMeasurement) {
		this.mafMeasurement = mafMeasurement;
	}


	public void setResultTime(long currentTimeMillis) {
		this.resultTime = currentTimeMillis;
	}


	public void setRpmMeasurement(int rpmMeasurement) {
		this.rpmMeasurement = rpmMeasurement;
	}


	public void setSpeedMeasurement(int speedMeasurement) {
		this.speedMeasurement = speedMeasurement;
	}
	
	
}
