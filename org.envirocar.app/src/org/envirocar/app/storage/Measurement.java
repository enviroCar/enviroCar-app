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

package org.envirocar.app.storage;

import org.envirocar.app.exception.LocationInvalidException;

/**
 * Measurement class that contains all the measured values
 * 
 * @author jakob
 * 
 */

public class Measurement {

	// All measurement values

	private int id;
	private float latitude;
	private float longitude;
	private long measurementTime;
	private int speed;
	private double maf;
	private double calculatedMaf;
	private double rpm;
	private double intake_temperature;
	private double intake_pressure;
	private Track track;

	/**
	 * Create a new measurement. Latitude AND longitude are not allowed to both
	 * equal 0.0. This method also sets the measurement time according to the
	 * System.currentTimeMillis() method.
	 * 
	 * @param latitude
	 *            Latitude of the measurement (WGS 84)
	 * @param longitude
	 *            Longitude of the measurement (WGS 84)
	 * @throws LocationInvalidException
	 *             If latitude AND longitude equal 0.0
	 */

	public Measurement(float latitude, float longitude)
			throws LocationInvalidException {
		if (latitude != 0.0 && longitude != 0.0) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.measurementTime = System.currentTimeMillis();
			this.speed = 0;
			this.maf = 0.0;
			this.rpm = 0;
			this.intake_temperature = 0;
			this.intake_pressure = 0;
			this.calculatedMaf = 0;
		} else {
			throw new LocationInvalidException();
		}
	}

	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Measurement [id=" + id + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", measurementTime="
				+ measurementTime + ", speed=" + speed + ", maf=" + maf
				+ ", track=" + track + "]";
	}



	/**
	 * @return the maf
	 */
	public double getMaf() {
		return maf;
	}

	/**
	 * @param maf
	 *            the maf to set
	 */
	public void setMaf(double maf) {
		this.maf = maf;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the latitude
	 */
	public float getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the measurementTime
	 */
	public long getMeasurementTime() {
		return measurementTime;
	}

	/**
	 * @param measurementTime
	 *            the measurementTime to set
	 */
	public void setMeasurementTime(long measurementTime) {
		this.measurementTime = measurementTime;
	}

	/**
	 * @return the speed
	 */
	public int getSpeed() {
		return speed;
	}

	/**
	 * @param speed
	 *            the speed to set
	 */
	public void setSpeed(int speed) {
		this.speed = speed;
	}

	/**
	 * @return the track
	 */
	public Track getTrack() {
		return track;
	}

	/**
	 * @param track
	 *            the track to set
	 */
	public void setTrack(Track track) {
		this.track = track;
	}
	
	/**
	 * @return the rpm
	 */
	public double getRpm() {
		return rpm;
	}

	/**
	 * @param maf
	 *            the rpm to set
	 */
	public void setRpm(double rpm) {
		this.rpm = rpm;
	}

	/**
	 * @return the intake_temperature
	 */
	public double getIntakeTemperature() {
		return intake_temperature;
	}

	/**
	 * @param intake_temperature
	 *            the intake_temperature to set
	 */
	public void setIntakeTemperature(double intake_temperature) {
		this.intake_temperature = intake_temperature;
	}

	/**
	 * @return the intake_pressure
	 */
	public double getIntakePressure() {
		return intake_pressure;
	}

	/**
	 * @param intake_pressure
	 *            the intake_pressure to set
	 */
	public void setIntakePressure(double intake_pressure) {
		this.intake_pressure = intake_pressure;
	}
	
	/**
	 * @return calculated maf
	 */
	public double getCalculatedMaf() {
		return calculatedMaf;
	}

	/**
	 * @param calculated maf
	 */
	public void setCalculatedMaf(double calculatedMaf) {
		this.calculatedMaf = calculatedMaf;
		
	}

}
