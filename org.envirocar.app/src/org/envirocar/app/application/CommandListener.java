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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.IntakePressureEvent;
import org.envirocar.app.event.IntakeTemperatureEvent;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.event.LocationEventListener;
import org.envirocar.app.event.RPMEvent;
import org.envirocar.app.event.SpeedEvent;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.exception.TracksException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.Utils;

import android.location.Location;
import android.net.ParseException;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link EventBus}.
 * 
 * @author matthes rieke
 *
 */
public class CommandListener implements Listener, LocationEventListener, MeasurementListener {
	
	private static final Logger logger = Logger.getLogger(CommandListener.class);

	private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

	private Track track;
	private String trackDescription = "Description of the track";
	private Car car;
	private DbAdapter dbAdapter;
	private Collector collector;
	private Location location;
	
	public CommandListener(Car car) {
		this.car = car;
		this.dbAdapter = DbAdapterImpl.instance();
		this.collector = new Collector(this, this.car);
		EventBus.getInstance().registerListener(this);
		logger.debug("Initialized. Hash: "+System.identityHashCode(this));
	}

	public void receiveUpdate(CommonCommand command) {
		logger.debug("update received");
		// Get the name and the result of the Command

		String commandName = command.getCommandName();
		String commandResult = command.getResult();
		if (isNoDataCommand(command))
			return;

		/*
		 * Check which measurent is returned and save the value in the
		 * previously created measurement
		 */

		// Speed

		if (commandName.equals(Speed.NAME)) {

			try {
				Integer speedMeasurement = Integer.valueOf(commandResult);
				this.collector.newSpeed(speedMeasurement);
				EventBus.getInstance().fireEvent(new SpeedEvent(speedMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("speed parse exception", e);
			}
		}
		
		//RPM
		
		else if (commandName.equals(RPM.NAME)) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer rpmMeasurement = Integer.valueOf(commandResult);
				this.collector.newRPM(rpmMeasurement);
				EventBus.getInstance().fireEvent(new RPMEvent(rpmMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("rpm parse exception", e);
			}
		}

		//IntakePressure
		
		else if (commandName.equals(IntakePressure.NAME)) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer intakePressureMeasurement = Integer.valueOf(commandResult);
				this.collector.newIntakePressure(intakePressureMeasurement);
				EventBus.getInstance().fireEvent(new IntakePressureEvent(intakePressureMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("Intake Pressure parse exception", e);
			}
		}
		
		//IntakeTemperature
		
		else if (commandName.equals(IntakeTemperature.NAME)) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer intakeTemperatureMeasurement = Integer.valueOf(commandResult);
				this.collector.newIntakeTemperature(intakeTemperatureMeasurement);
				EventBus.getInstance().fireEvent(new IntakeTemperatureEvent(intakeTemperatureMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("Intake Temperature parse exception", e);
			}
		}
						
		else if (commandName.equals(MAF.NAME)) {
			String maf = commandResult;

			try {
				NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
				Number number;
				number = format.parse(maf);
				double mafMeasurement = number.doubleValue();
				this.collector.newMAF(mafMeasurement);
			} catch (ParseException e) {
				logger.warn("parse exception maf", e);
			} catch (java.text.ParseException e) {
				logger.warn("parse exception maf", e);
			}
		}
		else {
			return;
		}

	}
	

	private boolean isNoDataCommand(CommonCommand command) {
		if (command.getRawData() != null && (command.getRawData().equals("NODATA") ||
				command.getRawData().equals(""))) return true;
		
		if (command.getResult() != null && (command.getResult().equals("NODATA") ||
				command.getResult().equals(""))) return true;
		
		if (command.getResult() == null || command.getRawData() == null) return true;
		
		return false;
	}


	/**
	 * Helper method to insert track measurement into the database (ensures that
	 * track measurement is only stored every 5 seconds and not faster...)
	 * 
	 * @param measurement
	 *            The measurement you want to insert
	 */
	public void insertMeasurement(Measurement measurement) {

		// TODO: This has to be added with the following conditions:
		/*
		 * 1)New measurement if more than 50 meters away 2)New measurement if
		 * last measurement more than 1 minute ago 3)New measurement if MAF
		 * value changed significantly (whatever this means... we will have to
		 * investigate on that. also it is not clear whether we should use this
		 * condition because we are vulnerable to noise from the sensor.
		 * therefore, we should include a minimum time between measurements (1
		 * sec) as well.)
		 */
		logger.info("inserting measurement to Track: "+track.getName());
		track.addMeasurement(measurement);
		dbAdapter.insertMeasurement(measurement);
		logger.info("Add new measurement to track: " + measurement.toString());
	}
	
	/**
	 * This method determines whether it is necessary to create a new track or
	 * of the current/last used track should be reused
	 */
	@Override
	public void createNewTrackIfNecessary() {
		logger.info("createNewTrackIfNecessary");
		// if track is null, create a new one or take the last one from the
		// database
		
		String date = format.format(new Date());

		if (track == null) {

			logger.info("The track was null");

			Track lastUsedTrack;

			try {
				lastUsedTrack = dbAdapter.getLastUsedTrack();

				try {

					// New track if last measurement is more than 60 minutes
					// ago

					if ((System.currentTimeMillis() - lastUsedTrack
							.getLastMeasurement().getTime()) > 3600000) {
						logger.info("I create a new track because the last measurement is more than 60 mins ago");
						track = new Track("123456", car, dbAdapter);
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						dbAdapter.updateTrack(track);
						logger.info("Using Track: "+ track.getName());
						return;
					}

					// new track if last position is significantly different
					// from the current position (more than 3 km)
					if (location == null || Utils.getDistance(lastUsedTrack.getLastMeasurement().getLatitude(),lastUsedTrack.getLastMeasurement().getLongitude(),
							location.getLatitude(), location.getLongitude()) > 3.0) {
						logger.info("The last measurement's position is more than 3 km away. I will create a new track");
						track = new Track("123456", car, dbAdapter); 
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						dbAdapter.updateTrack(track);
						logger.info("Using Track: "+ track.getName());
						return;

					}

					// TODO: New track if user clicks on create new track button

					// TODO: new track if VIN changed

					else {
						logger.info("I will append to the last track because that still makes sense");
						track = lastUsedTrack;
						logger.info("Using Track: "+ track.getName());
						return;
					}

				} catch (MeasurementsException e) {
					logger.warn("The last track contains no measurements. I will delete it and create a new one.");
					dbAdapter.deleteTrack(lastUsedTrack.getId());
					track = new Track("123456", car, dbAdapter); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					dbAdapter.updateTrack(track);
				}

			} catch (TracksException e) {
				logger.warn("There was no track in the database so I created a new one");
				track = new Track("123456", car, dbAdapter); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				dbAdapter.updateTrack(track);
			}

			logger.info("Using Track: "+ track.getName());
			return;

		}

		// if track is not null, determine whether it is useful to create a new
		// track and store the current one

		if (track != null) {

			logger.info("the track was not null");

			Track currentTrack = track;

			try {

				// New track if last measurement is more than 60 minutes
				// ago
				if ((System.currentTimeMillis() - currentTrack
						.getLastMeasurement().getTime()) > 3600000) {
					track = new Track("123456", car, dbAdapter);
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					dbAdapter.updateTrack(track);
					logger.info("I create a new track because the last measurement is more than 60 mins ago");
					logger.info("Using Track: "+ track.getName());
					return;
				}
				// TODO: New track if user clicks on create new track button

				// new track if last position is significantly different from
				// the
				// current position (more than 3 km)

				if (Utils.getDistance(currentTrack.getLastMeasurement().getLatitude(),currentTrack.getLastMeasurement().getLongitude(),
						location.getLatitude(), location.getLongitude()) > 3.0) {
					track = new Track("123456", car, dbAdapter); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					dbAdapter.updateTrack(track);
					logger.info("The last measurement's position is more than 3 km away. I will create a new track");
					logger.info("Using Track: "+ track.getName());
					return;

				}

				// TODO: new track if VIN changed

				else {
					logger.info("I will append to the last track because that still makes sense");
					logger.info("Using Track: "+ track.getName());
					return;
				}

			} catch (MeasurementsException e) {
				logger.warn("The last track contains no measurements. I will delete it and create a new one.");
				dbAdapter.deleteTrack(currentTrack.getId());
				track = new Track("123456", car, dbAdapter); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				dbAdapter.updateTrack(track);
				logger.info("Using Track: "+ track.getName());
			}
		}
		
	}


	@Override
	public void resetTrack() {
		logger.info("resetTrack");
		this.track = null;
	}

	@Override
	public void receiveEvent(LocationEvent event) {
		this.collector.newLocation(event.getPayload());
		this.location = event.getPayload();
	}


	
}
