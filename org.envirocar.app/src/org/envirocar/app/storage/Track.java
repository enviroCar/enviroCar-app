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

import static org.envirocar.app.storage.Measurement.PropertyKey.CONSUMPTION;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.model.TrackId;
import org.envirocar.app.protocol.algorithm.AbstractConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.BasicConsumptionAlgorithm;
import org.envirocar.app.protocol.algorithm.UnsupportedFuelTypeException;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is a track. A track is a collection of measurements. All measurements of
 * a track are accessible via the track. The track stores meta information about
 * the ride (car, fuel, description...)
 * 
 */
public class Track implements Comparable<Track> {
	
	public enum TrackStatus {
		ONGOING {
			@Override
			public String toString() {
				return "ONGOING";
			}
			
		},
		
		FINISHED {
			@Override
			public String toString() {
				return "FINISHED";
			}
		}
		
	}
	
	private static final Logger logger = Logger.getLogger(Track.class);

	private String name;
	private String description;
	private List<Measurement> measurements = new ArrayList<Measurement>();
	private Car car;
	private AbstractConsumptionAlgorithm consumptionAlgorithm;
	private String remoteID;
	private Double consumptionPerHour;
	private TrackStatus status = TrackStatus.ONGOING;

	private DbAdapter dbAdapter;

	private boolean lazyLoadingMeasurements;

	private Long startTime = null;
	private Long endTime = null;

	private TrackMetadata metadata;

	private TrackId trackId;

	public static Track createTrackWithId(long id, DbAdapter dbAdapterImpl) {
		Track track = new Track(id);
		track.dbAdapter = dbAdapterImpl;
		return track;
	}
	
	public static Track createNewLocalTrack(DbAdapter dbAdapterImpl) {
		Track t = new Track(dbAdapterImpl);
		return t;
	}
	
	
	public static Track createRemoteTrack(String remoteID, DbAdapter dbAdapter) {
		Track track = new Track(remoteID, dbAdapter);
		return track;
	}

	private Track(long id) {
		this.trackId = new TrackId(id);
	}
	
	private Track(String remoteID, DbAdapter dbAdapter) {
		this(dbAdapter);
		this.remoteID = remoteID;
		this.status = TrackStatus.FINISHED;
	}
	
	/**
	 * Constructor for creating "fresh" new track. Use this for new measurements
	 * that were captured from the OBD-II adapter.
	 */
	private Track(DbAdapter dbAdapter) {
		this.name = "";
		this.description = "";
		this.measurements = new ArrayList<Measurement>();
		this.trackId = new TrackId(dbAdapter.insertTrack(this));
		this.dbAdapter = dbAdapter;
	}

	/**
	 * @return the localTrack
	 */
	public boolean isLocalTrack() {
		return !isRemoteTrack();
	}

	public boolean isRemoteTrack() {
		return (remoteID != null ? true : false);
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
	public List<Measurement> getMeasurements() {
		if ((measurements == null || measurements.isEmpty()) && dbAdapter != null) {
			try {
				this.measurements = dbAdapter.getAllMeasurementsForTrack(this);
			} catch (FinishedTrackWithoutMeasurementsException e) {
				logger.warn(e.getMessage(), e);
			}
		}
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
	public Long getStartTime() throws MeasurementsException {
		if (startTime != null) return startTime;
		
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(0).getTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}

	public void setStartTime(Long time) {
		this.startTime = time;
	}
	
	/**
	 * get the time where the track ended
	 * 
	 * @return end time of track as unix long
	 * @throws MeasurementsException
	 */
	public Long getEndTime() throws MeasurementsException {
		if (endTime != null) return endTime;
		
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(this.getMeasurements().size() - 1).getTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}
	
	public void setEndTime(Long time) {
		this.endTime = time;
	}
	
	/**
	 * Sets the measurements with an arraylist of measurements
	 * 
	 * @param measurements
	 *            the measurements of a track
	 */
	public void setMeasurementsAsArrayList(List<Measurement> measurements) {
		setMeasurementsAsArrayList(measurements, false);
	}
	

	public void setMeasurementsAsArrayList(
			List<Measurement> measurements, boolean storeInDb) {
		this.measurements = measurements;
		
		if (storeInDb) {
			storeMeasurementsInDbAdapter();
		}
	}
	
	private void storeMeasurementsInDbAdapter() {
		if (this.dbAdapter != null) {
			for (Measurement measurement : measurements) {
				try {
					this.dbAdapter.insertMeasurement(measurement);
				} catch (MeasurementsException e) {
					logger.warn(e.getMessage(), e);
				} catch (TrackAlreadyFinishedException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Use this method only to insert "fresh" measurements, not to recreate a
	 * Track from the database Use
	 * {@code insertMeasurement(ArrayList<Measurement> measurements)} instead
	 * Inserts measurments into the Track and into the database!
	 * 
	 * @param measurement
	 * @throws TrackAlreadyFinishedException 
	 * @throws MeasurementsException 
	 */
	public void addMeasurement(Measurement measurement) throws TrackAlreadyFinishedException {
		measurement.setTrackId(this.trackId);
		
		/*
		 * we do NOT need to add the measurement to the list.
		 * this blows up memory usage and this instance of Track
		 * is never used elsewhere nor its getMeasurements() method
		 * and the like.
		 */
//		this.measurements.add(measurement);
		
		if (this.dbAdapter != null) {
			try {
				this.dbAdapter.insertNewMeasurement(measurement);
			} catch (MeasurementsException e) {
				logger.severe("This should never happen", e);
				return;
			}	
		} else {
			logger.warn("DbAdapter was null! Could not insert measurement");
		}
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
		return trackId.getId();
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
		List<Measurement> measurements = this.getMeasurements();

		double distance = 0.0;

		if (measurements.size() > 1) {
			for (int i = 0; i < measurements.size() - 1; i++) {
				distance = distance + Util.getDistance(measurements.get(i).getLatitude(), measurements.get(i).getLongitude(), measurements.get(i + 1).getLatitude(), measurements.get(i + 1).getLongitude());
			}
		}
		return distance;
	}

	/**
	 * Returns the last measurement of this track
	 * 
	 * @return the last measurement or null if there are no measurements
	 */
	public Measurement getLastMeasurement() {
		if (this.measurements.size() > 0) {
			return this.measurements.get(this.measurements.size() - 1);
		}
		else if (this.lazyLoadingMeasurements) {
			
		}
		return null;
	}
	
	/**
	 * Returns the first measurement of this track
	 * 
	 * @return Returns the last measurement or null if there are no measurements
	 */
	public Measurement getFirstMeasurement() {
		if (this.measurements.size() > 0) {
			return this.measurements.get(0);
		}
		return null;
	}
	
	/**
	 * Returns the average co2 emission for the track.
	 * 
	 * @return
	 */
	public double getCO2Average() {
		double co2Average = 0.0;
		try {
			for (Measurement measurement : measurements) {
				if (measurement.getProperty(CONSUMPTION) != null){
					co2Average = co2Average + consumptionAlgorithm.calculateCO2FromConsumption(measurement.getProperty(CONSUMPTION));
				}
			}
			co2Average = co2Average / measurements.size();
		} catch (FuelConsumptionException e) {
			logger.warn(e.getMessage(), e);
		}
		return co2Average;
	}
	
	public double getFuelConsumptionPerHour() throws UnsupportedFuelTypeException {
		if (consumptionPerHour == null) {
			consumptionPerHour = 0.0;
			
			int consideredCount = 0;
			for (int i = 0; i < measurements.size(); i++) {
				try {
					consumptionPerHour = consumptionPerHour + consumptionAlgorithm.calculateConsumption(measurements.get(i));
					consideredCount++;
				} catch (FuelConsumptionException e) {
					logger.warn(e.getMessage());
				}
			}
			consumptionPerHour = consumptionPerHour / consideredCount;
			
		}
		return consumptionPerHour;
	}

	@Override
	public int compareTo(Track t) {
		try {
			if (t.getStartTime() == null && t.getEndTime() == null) {
				/*
				 * we cannot assume any ordering
				 */
				return 0;
			}
		}
		catch (MeasurementsException e) {
			return 0;
		}
			
		try {
			if (this.getStartTime() == null) {
				/*
				 * no measurements, this is probably a relatively new track
				 */
				return -1;
			}
		}
		catch (MeasurementsException e) {
			return -1;
		}
		
		try {
			if (t.getStartTime() == null) {
				/*
				 * no measurements, that is probably a relatively new track
				 */
				return 1;
			}
		}
		catch (MeasurementsException e) {
			return 1;
		}

		try {
			return (this.getStartTime() < t.getStartTime() ? 1 : -1);
		} catch (MeasurementsException e) {
			return 0;
		}	
		
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

	public void setStatus(TrackStatus s) {
		this.status = s;
	}

	public TrackStatus getStatus() {
		return status;
	}

	public void setLazyLoadingMeasurements(boolean b) {
		this.lazyLoadingMeasurements = b;
	}

	public boolean isLazyLoadingMeasurements() {
		return lazyLoadingMeasurements;
	}

	/**
	 * Creates a Track and adds its contents to the DB layer (adapter).
	 * 
	 * @param json the input json object
	 * @param adapter the DB layer adapter
	 * @return the Track object
	 * @throws JSONException parsing fails or contains unexpected properties
	 * @throws ParseException if DateTime parsing fails
	 */
	public static Track fromJson(JSONObject json, DbAdapter adapter) throws JSONException, ParseException {
		JSONObject trackProperties = json.getJSONObject("properties");
		Track t = Track.createRemoteTrack(trackProperties.getString("id"), adapter);
		String trackName = "unnamed Track #"+t.getId();
		try {
			trackName = trackProperties.getString("name");
		} catch (JSONException e){
			logger.warn(e.getMessage(), e);
		}
		
		t.setName(trackName);
		String description = "";
		try {
			description = trackProperties.getString("description");
		} catch (JSONException e){
			logger.warn(e.getMessage(), e);
		}
		
		t.setDescription(description);
		JSONObject sensorProperties = trackProperties.getJSONObject("sensor").getJSONObject("properties");
		
		t.setCar(Car.fromJson(sensorProperties)); 
		//include server properties tracks created, modified?
		
		t.dbAdapter.updateTrack(t);
		//Log.i("track_id",t.getId()+" "+((DbAdapterRemote) dbAdapter).trackExistsInDatabase(t.getId())+" "+dbAdapter.getNumberOfStoredTracks());
		
		Measurement recycleMeasurement;
		
		List<Measurement> measurements = new ArrayList<Measurement>();
		JSONArray features = json.getJSONArray("features");
		logger.info("Parsing measurements of track "+t.getRemoteID()+". Count: "+features.length());
		for (int j = 0; j < features.length(); j++) {
			JSONObject measurementJsonObject = features.getJSONObject(j);
			recycleMeasurement = Measurement.fromJson(measurementJsonObject);
			
			recycleMeasurement.setTrackId(t.trackId);
			measurements.add(recycleMeasurement);
		}
		
		t.setMeasurementsAsArrayList(measurements);
		logger.info("Storing measurements in database");
		t.storeMeasurementsInDbAdapter();
		return t;
	}

	public boolean isFinished() {
		return status != null && status == TrackStatus.FINISHED;
	}

	/**
	 * updates the tracks metadata. if there is already metadata,
	 * the properties are merged. the provided object overrides
	 * existing keys.
	 * 
	 * @param newMetadata
	 */
	public void updateMetadata(TrackMetadata newMetadata) {
		if (this.metadata != null) {
			this.metadata.merge(newMetadata);
		}
		else {
			setMetadata(newMetadata);
		}
		
		dbAdapter.updateTrack(this);
	}

	public TrackMetadata getMetadata() {
		return this.metadata;
	}

	public void setMetadata(TrackMetadata m) {
		this.metadata = m;
	}

	@Override
	public String toString() {
		return "Track / id: "+getId() +" / Name: "+getName();
	}

}
