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

import java.util.HashMap;
import java.util.Map;

import org.envirocar.app.exception.LocationInvalidException;

/**
 * Measurement class that contains all the measured values
 * 
 * @author jakob
 * 
 */

public class Measurement {

	// All measurement values
	public enum PropertyKey {
		SPEED {
			public String toString() {
		        return "Speed";
		    }
		},
		MAF {
			public String toString() {
		        return "MAF";
		    } 
		}, 
		CALCULATED_MAF {
			public String toString() {
		        return "Calculated MAF";
		    } 
		}, 
		RPM {
			public String toString() {
		        return "Rpm";
		    } 
		}, 
		INTAKE_TEMPERATURE {
			public String toString() {
		        return "Intake Temperature";
		    } 
		}, 
		INTAKE_PRESSURE {
			public String toString() {
		        return "Intake Pressure";
		    } 
		},
		CO2 {
			public String toString() {
		        return "CO2";
		    } 
		},
		CONSUMPTION {
			public String toString() {
		        return "Consumption";
		    } 
		}
	}

	private int id;
	private double latitude;
	private double longitude;
	private long time;
	private Track track;
	
	private Map<PropertyKey, Double> propertyMap = new HashMap<PropertyKey, Double>();

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

	public Measurement(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.time = System.currentTimeMillis();
	}
	
	public void addProperty(PropertyKey key, Double value) {
		propertyMap.put(key, value);
	}

	public Double getProperty(PropertyKey key) {
		return (Double) propertyMap.get(key);
	}
	
	public Map<PropertyKey, Double> getAllProperties() {
		return propertyMap;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Measurement [");
		sb.append("id=" + id + ",");
		sb.append("latitude=" + latitude + ",");
		sb.append("longitude=" + longitude + ",");
		sb.append("time=" + time + ",");
		sb.append("id=" + id + ",");
		sb.append("id=" + id + ",");
		for (PropertyKey key : propertyMap.keySet()) {
			sb.append(key.toString() + "=" + propertyMap.get(key) + ",");
		}
		return sb.toString();
	}

	/**
	 * @return the maf
	 */
	@Deprecated
	public double getMaf() {
		return (Double) propertyMap.get(PropertyKey.MAF);
	}

	/**
	 * @param maf
	 *            the maf to set
	 */
	@Deprecated
	public void setMaf(double maf) {
		propertyMap.put(PropertyKey.MAF, maf);
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
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the measurementTime
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param measurementTime
	 *            the measurementTime to set
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @return the speed
	 */
	@Deprecated
	public int getSpeed() {
		return Double.valueOf((Double) propertyMap.get(PropertyKey.SPEED)).intValue() ;
	}

	/**
	 * @param speed
	 *            the speed to set
	 */
	@Deprecated
	public void setSpeed(int speed) {
		propertyMap.put(PropertyKey.SPEED, Double.valueOf(speed));
	}

	/**
	 * @return the track
	 */
	@Deprecated
	public Track getTrack() {
		return track;
	}

	/**
	 * @param track
	 *            the track to set
	 */
	@Deprecated
	public void setTrack(Track track) {
		this.track = track;
	}
	
	/**
	 * @return the rpm
	 */
	@Deprecated
	public int getRpm() {
		return Double.valueOf((Double) propertyMap.get(PropertyKey.RPM)).intValue();
	}

	/**
	 * @param maf
	 *            the rpm to set
	 */
	@Deprecated
	public void setRpm(int rpm) {
		propertyMap.put(PropertyKey.RPM, Double.valueOf(rpm));
	}

	/**
	 * @return the intake_temperature
	 */
	@Deprecated
	public double getIntakeTemperature() {
		return (Double) propertyMap.get(PropertyKey.INTAKE_TEMPERATURE);
	}

	/**
	 * @param intake_temperature
	 *            the intake_temperature to set
	 */
	@Deprecated
	public void setIntakeTemperature(double intake_temperature) {
		propertyMap.put(PropertyKey.INTAKE_TEMPERATURE, intake_temperature);
	}

	/**
	 * @return the intake_pressure
	 */
	@Deprecated
	public double getIntakePressure() {
		return (Double) propertyMap.get(PropertyKey.INTAKE_PRESSURE);
	}

	/**
	 * @param intake_pressure
	 *            the intake_pressure to set
	 */
	@Deprecated
	public void setIntakePressure(double intake_pressure) {
		propertyMap.put(PropertyKey.INTAKE_PRESSURE, intake_pressure);
	}
	
	/**
	 * @return calculated maf
	 */
	@Deprecated
	public double getCalculatedMaf() {
		return (Double) propertyMap.get(PropertyKey.CALCULATED_MAF);
	}

	/**
	 * @param calculated maf
	 */
	@Deprecated
	public void setCalculatedMaf(double calculatedMaf) {
		propertyMap.put(PropertyKey.CALCULATED_MAF, calculatedMaf);
	}

	@Deprecated
	public void setCO2(double c) {
		propertyMap.put(PropertyKey.CO2, c);
	}

	@Deprecated
	public double getCO2() {
		return (Double) propertyMap.get(PropertyKey.CO2);
	}

	@Deprecated
	public void setConsumption(double c) {
		propertyMap.put(PropertyKey.CONSUMPTION, c);
	}

	@Deprecated
	public double getConsumption() {
		return (Double) propertyMap.get(PropertyKey.CONSUMPTION);
	}

	public boolean hasProperty(PropertyKey key) {
		return propertyMap.containsKey(key);
	}

}
