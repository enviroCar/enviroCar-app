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

import java.util.ArrayList;

import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.protocol.AbstractConsumptionAlgorithm;
import org.envirocar.app.protocol.BasicConsumptionAlgorithm;
import org.envirocar.app.views.Utils;

/**
 * This is a track. A track is a collection of measurements. All measurements of
 * a track are accessible via the track. The track stores meta information about
 * the ride (car, fuel, description...)
 * 
 */
public class Track implements Comparable<Track> {
	
	private static final Logger logger = Logger.getLogger(Track.class);

	private long id;
	private String name;
	private String description;
	private ArrayList<Measurement> measurements = new ArrayList<Measurement>();
	private Car car;
	private AbstractConsumptionAlgorithm consumptionAlgorithm;
	private String vin;
	private String remoteID;
	private Double consumptionPerHour;

	/**
	 * @return the localTrack
	 */
	public boolean isLocalTrack() {
		return (remoteID == null ? true : false);
	}

	public boolean isRemoteTrack() {
		return (remoteID != null ? true : false);
	}
	
	public static Track createDbTrack(long id) {
		Track track = new Track(id);
		return track;
	}
	
	private Track(long id) {
		this.id = id;
	}
	
	public static Track createRemoteTrack(String remoteID) {
		Track track = new Track(remoteID);
		return track;
	}
	
	private Track(String remoteID) {
		this.remoteID = remoteID;
	}
	
	/**
	 * Constructor for creating "fresh" new track. Use this for new measurements
	 * that were captured from the OBD-II adapter.
	 */
	public Track(String vin, Car car, DbAdapter dbAdapter) {
		this.vin = vin;
		this.name = "";
		this.description = "";
		this.measurements = new ArrayList<Measurement>();
		this.car = car;
		this.consumptionAlgorithm = new BasicConsumptionAlgorithm(car);
		id = dbAdapter.insertTrack(this);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the measurements
	 */
	public ArrayList<Measurement> getMeasurements() {
		return measurements;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
		this.consumptionAlgorithm = new BasicConsumptionAlgorithm(car);
	}

	/**
	 * get the time where the track started
	 * 
	 * @return start time of track as unix long
	 * @throws MeasurementsException
	 */
	public long getStartTime() throws MeasurementsException {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(0).getTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}

	/**
	 * get the time where the track ended
	 * 
	 * @return end time of track as unix long
	 * @throws MeasurementsException
	 */
	public long getEndTime() throws MeasurementsException {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(this.getMeasurements().size() - 1).getTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}

	/**
	 * Sets the measurements with an arraylist of measurements
	 * 
	 * @param measurements
	 *            the measurements of a track
	 */
	public void setMeasurementsAsArrayList(ArrayList<Measurement> measurements) {
		this.measurements = measurements;
	}

	/**
	 * Use this method only to insert "fresh" measurements, not to recreate a
	 * Track from the database Use
	 * {@code insertMeasurement(ArrayList<Measurement> measurements)} instead
	 * Inserts measurments into the Track and into the database!
	 * 
	 * @param measurement
	 */
	public void addMeasurement(Measurement measurement) {
		measurement.setTrack(Track.this);
		this.measurements.add(measurement);
	}

	/**
	 * Returns the number of measurements of this track
	 * 
	 * @return
	 */
	public int getNumberOfMeasurements() {
		return this.measurements.size();
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * get the VIN
	 * 
	 * @return
	 */
	public String getVin() {
		return vin;
	}

	/**
	 * set the vin
	 * 
	 * @param vin
	 */
	public void setVin(String vin) {
		this.vin = vin;
	}

	public String getRemoteID() {
		return remoteID;
	}

	public void setRemoteID(String remoteID) {
		this.remoteID = remoteID;
	}

	/**
	 * Returns the length of a track in kilometers
	 * 
	 * @return
	 */
	public double getLengthOfTrack() {
		ArrayList<Measurement> measurements = this.getMeasurements();

		double distance = 0.0;

		if (measurements.size() > 1) {
			for (int i = 0; i < measurements.size() - 1; i++) {
				distance = distance + Utils.getDistance(measurements.get(i).getLatitude(), measurements.get(i).getLongitude(), measurements.get(i + 1).getLatitude(), measurements.get(i + 1).getLongitude());
			}
		}
		return distance;
	}

	/**
	 * Returns the last measurement of this track
	 * 
	 * @return
	 * @throws MeasurementsException
	 *             If there are no measurements in the track
	 */
	public Measurement getLastMeasurement() throws MeasurementsException {
		if (this.measurements.size() > 0) {
			return this.measurements.get(this.measurements.size() - 1);
		} else
			throw new MeasurementsException("No Measurements in this track!");
	}
	
	/**
	 * Returns the first measurement of this track
	 * 
	 * @return
	 * @throws MeasurementsException
	 *             If there are no measurements in the track
	 */
	public Measurement getFirstMeasurement() throws MeasurementsException {
		if (this.measurements.size() > 0) {
			return this.measurements.get(0);
		} else
			throw new MeasurementsException("No measurements in this track!");
	}
	
	/**
	 * Returns the average co2 emission for the track.
	 * 
	 * @return
	 */
	public double getCO2Average() {
		double co2Average = 0.0;
		try {
			for (int i = 0; i < measurements.size(); i++) {
				co2Average = co2Average + consumptionAlgorithm.calculateCO2FromConsumption(measurements.get(i).getConsumption());
			}
			co2Average = co2Average / measurements.size();
		} catch (FuelConsumptionException e) {
			logger.warn(e.getMessage(), e);
		}
		return co2Average;
	}
	
	public double getFuelConsumptionPerHour() {
		if (consumptionPerHour == null) {
			consumptionPerHour = 0.0;
			try {
				for (int i = 0; i < measurements.size(); i++) {
					consumptionPerHour = consumptionPerHour + consumptionAlgorithm.calculateConsumption(measurements.get(i));
				}
				consumptionPerHour = consumptionPerHour / measurements.size();
			} catch (FuelConsumptionException e) {
				logger.warn(e.getMessage(),e);
			}
		}
		return consumptionPerHour;
	}

	@Override
	public int compareTo(Track t) {
		try {
			return (this.getFirstMeasurement().getTime() < t.getFirstMeasurement().getTime() ? 1 : -1);
		} catch (MeasurementsException e) {
			logger.warn(e.getMessage(), e);
		} 
		return 0;
	}

	public double getLiterPerHundredKm() throws MeasurementsException {
		return consumptionPerHour * getDurationInMillis() / (1000 * 60 * 60) / getLengthOfTrack() * 100;
	}

	public long getDurationInMillis() throws MeasurementsException {
		return getEndTime() - getStartTime();
	}

	public double getGramsPerKm() throws FuelConsumptionException, MeasurementsException {

		if (this.car.getFuelType().equals(FuelType.GASOLINE)) {
			return getLiterPerHundredKm() * 23.3;
		} else if (this.car.getFuelType().equals(FuelType.DIESEL)) {
			return getLiterPerHundredKm() * 26.4;
		} else
			throw new FuelConsumptionException();
	}

}
