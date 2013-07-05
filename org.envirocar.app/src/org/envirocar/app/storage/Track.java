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
import org.envirocar.app.views.Utils;

/**
 * This is a track. A track is a collection of measurements. All measurements of
 * a track are accessible via the track. The track stores meta information about
 * the ride (car, fuel, description...)
 * 
 */
public class Track {

	private String id;
	private String name;
	private String description;
	private ArrayList<Measurement> measurements;
	private String carManufacturer;
	private String carModel;
	private String vin;
	private String fuelType;
	private String sensorID;
	private boolean localTrack;

	/**
	 * @return the localTrack
	 */
	public boolean isLocalTrack() {
		return localTrack;
	}

	/**
	 * @param localTrack
	 *            the localTrack to set
	 */
	public void setLocalTrack(boolean localTrack) {
		this.localTrack = localTrack;
	}

	/**
	 * @return the sensorID
	 */
	public String getSensorID() {
		return sensorID;
	}

	/**
	 * @param sensorID
	 *            the sensorID to set
	 */
	public void setSensorID(String sensorID) {
		this.sensorID = sensorID;
	}

	private DbAdapter dbAdapter;

	/**
	 * Constructor for creating a Track from the Database. Use this constructor
	 * when you want to rebuild tracks from the database.
	 */
	public Track(String id) {
		this.id = id;
		this.name = "";
		this.description = "";
		this.carManufacturer = "";
		this.carModel = "";
		this.vin = "";
		this.fuelType = "";
		this.sensorID = "";
		this.measurements = new ArrayList<Measurement>();
	}

	/**
	 * Constructor for creating "fresh" new track. Use this for new measurements
	 * that were captured from the OBD-II adapter.
	 */
	public Track(String vin, String fuelType, String carManufacturer, String carModel, String sensorId, DbAdapter dbAdapter) {
		this.vin = vin;
		this.name = "";
		this.description = "";
		this.carManufacturer = carManufacturer;
		this.carModel = carModel;
		this.fuelType = fuelType;
		this.sensorID = sensorId;
		this.measurements = new ArrayList<Measurement>();
		this.dbAdapter = dbAdapter;
		id = String.valueOf(dbAdapter.insertTrack(this));
	}

	/**
	 * Set the db adpater for the track. This method is needed when you want to
	 * update a track in the database because when you get the track, the
	 * dbadapter is not returned.
	 * 
	 * @param dbAdapter
	 *            the dbapapter
	 */
	public void setDatabaseAdapter(DbAdapter dbAdapter) {
		this.dbAdapter = dbAdapter;
	}

	/**
	 * Updates the Track in the database
	 * 
	 * @return
	 */
	public boolean commitTrackToDatabase() {
		return dbAdapter.updateTrack(this);
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
	 * @return the carManufacturer
	 */
	public String getCarManufacturer() {
		return carManufacturer;
	}

	/**
	 * @param carManufacturer
	 *            the carManufacturer to set
	 */
	public void setCarManufacturer(String carManufacturer) {
		this.carManufacturer = carManufacturer;
	}

	/**
	 * @return the carModel
	 */
	public String getCarModel() {
		return carModel;
	}

	/**
	 * @param carModel
	 *            the carModel to set
	 */
	public void setCarModel(String carModel) {
		this.carModel = carModel;
	}

	/**
	 * @return the measurements
	 */
	public ArrayList<Measurement> getMeasurements() {
		return measurements;
	}

	/**
	 * get the time where the track started
	 * 
	 * @return start time of track as unix long
	 * @throws MeasurementsException
	 */
	public long getStartTime() throws MeasurementsException {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(0).getMeasurementTime();
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
			return this.getMeasurements().get(this.getMeasurements().size() - 1).getMeasurementTime();
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
		dbAdapter.insertMeasurement(measurement);
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
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
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

	/**
	 * set the fuel type
	 * 
	 * @param fuelType
	 */
	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	/**
	 * get the fuel type
	 * 
	 * @return
	 */
	public String getFuelType() {
		return fuelType;
	}

	/**
	 * Returns the fuel consumption for a measurement
	 * 
	 * @param measurement
	 *            The measurement with the fuel consumption
	 * @return The fuel consumption in l/s. 0.0 if MAF is -1.0 (no MAF sensor)
	 * @throws FuelConsumptionException
	 */

	public double getFuelConsumptionOfMeasurement(int measurement) throws FuelConsumptionException {

		Measurement m = getMeasurements().get(measurement);

		double maf = m.getMaf();

		if (maf != -1.0) {
			if (this.fuelType.equals("Gasoline")) {
				return (maf / 14.7) / 747;
			} else if (this.fuelType.equals("Diesel")) {
				return (maf / 14.5) / 832;
			} else
				throw new FuelConsumptionException();
		} else {
			return 0.0;
		}

	}

	/**
	 * Returns the Co2 emission of a measurement
	 * 
	 * @param measurement
	 * @return co2 emission in kg/s
	 * @throws FuelConsumptionException
	 */
	public double getCO2EmissionOfMeasurement(int measurement) throws FuelConsumptionException {

		double fuelCon;
		fuelCon = getFuelConsumptionOfMeasurement(measurement);

		if (this.fuelType.equals("Gasoline")) {
			return fuelCon * 2.35;
		} else if (this.fuelType.equals("Diesel")) {
			return fuelCon * 2.65;
		} else
			throw new FuelConsumptionException();

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
	 * Returns the first measurement of this grack
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

}
