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

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.EngineLoad;
import org.envirocar.app.commands.FuelSystemStatus;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.LongTermTrimBank1;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.NumberResultCommand;
import org.envirocar.app.commands.O2LambdaProbe;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.ShortTermTrimBank1;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.commands.TPS;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.GpsDOP;
import org.envirocar.app.event.GpsDOPEvent;
import org.envirocar.app.event.GpsDOPEventListener;
import org.envirocar.app.event.IntakePressureEvent;
import org.envirocar.app.event.IntakeTemperatureEvent;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.event.LocationEventListener;
import org.envirocar.app.event.RPMEvent;
import org.envirocar.app.event.SpeedEvent;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.app.storage.TrackAlreadyFinishedException;
import org.envirocar.app.storage.TrackMetadata;
import org.envirocar.app.util.Util;

import android.location.Location;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link EventBus}.
 * 
 * @author matthes rieke
 *
 */
public class CommandListener implements Listener, LocationEventListener, MeasurementListener {
	
	private static final Logger logger = Logger.getLogger(CommandListener.class);

	private static final long MAX_TIME_BETWEEN_MEASUREMENTS = 1000 * 60 * 15;

	private static final double MAX_DISTANCE_BETWEEN_MEASUREMENTS = 3.0;

	private static final int MAX_CREATION_TRIES = 10;

	private Track track;
	private Collector collector;
	private Location location;

	private GpsDOPEventListener dopListener;

	private int trackCreationTries;

	private TrackMetadata trackMetadata;

	private boolean shutdownCompleted = false;

	private static int instanceCount;
	
	public CommandListener(Car car) {
		this.collector = new Collector(this, car);
		EventBus.getInstance().registerListener(this);
		dopListener = new GpsDOPEventListener() {
			@Override
			public void receiveEvent(GpsDOPEvent event) {
				GpsDOP dop = event.getPayload();
				collector.newDop(dop);
			}
		};
		EventBus.getInstance().registerListener(dopListener);
		
		synchronized (CommandListener.class) {
			instanceCount++;
			logger.debug("Initialized. Hash: "+System.identityHashCode(this) +"; active instances: "+instanceCount);
		}
		
	}

	public void receiveUpdate(CommonCommand command) {
		// Get the name and the result of the Command
		
		if (!(command instanceof NumberResultCommand)) return;
		
		NumberResultCommand numberCommand = (NumberResultCommand) command;

		if (isNoDataCommand(command))
			return;

		/*
		 * Check which measurent is returned and save the value in the
		 * previously created measurement
		 */

		// Speed

		if (command instanceof Speed) {

			try {
				Integer speedMeasurement = (Integer) numberCommand.getNumberResult();
				this.collector.newSpeed(speedMeasurement);
				EventBus.getInstance().fireEvent(new SpeedEvent(speedMeasurement));
				logger.info("Processed Speed Response: "+speedMeasurement +" time: "+command.getResultTime());
			} catch (NumberFormatException e) {
				logger.warn("speed parse exception", e);
			}
		}
		
		//RPM
		
		else if (command instanceof RPM) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer rpmMeasurement = (Integer) numberCommand.getNumberResult();
				this.collector.newRPM(rpmMeasurement);
				EventBus.getInstance().fireEvent(new RPMEvent(rpmMeasurement));
				logger.info("Processed RPM Response: "+rpmMeasurement +" time: "+command.getResultTime());
			} catch (NumberFormatException e) {
				logger.warn("rpm parse exception", e);
			}
		}

		//IntakePressure
		
		else if (command instanceof IntakePressure) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer intakePressureMeasurement = (Integer) numberCommand.getNumberResult();
				this.collector.newIntakePressure(intakePressureMeasurement);
				EventBus.getInstance().fireEvent(new IntakePressureEvent(intakePressureMeasurement));
				logger.info("Processed IAP Response: "+intakePressureMeasurement +" time: "+command.getResultTime());
			} catch (NumberFormatException e) {
				logger.warn("Intake Pressure parse exception", e);
			}
		}
		
		//IntakeTemperature
		
		else if (command instanceof IntakeTemperature) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				Integer intakeTemperatureMeasurement = (Integer) numberCommand.getNumberResult();
				this.collector.newIntakeTemperature(intakeTemperatureMeasurement);
				EventBus.getInstance().fireEvent(new IntakeTemperatureEvent(intakeTemperatureMeasurement));
				logger.info("Processed IAT Response: "+intakeTemperatureMeasurement +" time: "+command.getResultTime());
			} catch (NumberFormatException e) {
				logger.warn("Intake Temperature parse exception", e);
			}
		}
						
		else if (command instanceof MAF) {
			float mafMeasurement = (Float) numberCommand.getNumberResult();
			this.collector.newMAF(mafMeasurement);
			logger.info("Processed MAF Response: "+mafMeasurement +" time: "+command.getResultTime());
		}
		
		
		else if (command instanceof TPS) {
			int tps = (Integer) numberCommand.getNumberResult();
			this.collector.newTPS(tps);
			logger.info("Processed TPS Response: "+tps +" time: "+command.getResultTime());
		}

		else if (command instanceof EngineLoad) {
			double load = (Float) numberCommand.getNumberResult();
			this.collector.newEngineLoad(load);
			logger.info("Processed EngineLoad Response: "+load +" time: "+command.getResultTime());
		}
		
		else if (command instanceof FuelSystemStatus) {
			boolean loop = ((FuelSystemStatus) command).isInClosedLoop();
			int status = ((FuelSystemStatus) command).getStatus();
			this.collector.newFuelSystemStatus(loop, status);
			logger.info("Processed FuelSystemStatus Response: Closed? "+loop +" Status: "+ status +"; time: "+command.getResultTime());
		}
		
		else if (command instanceof O2LambdaProbe) {
			this.collector.newLambdaProbeValue((O2LambdaProbe) command);
			logger.info("Processed O2LambdaProbe Response: "+ command.toString());
		}
		
		else if (command instanceof ShortTermTrimBank1) {
			this.collector.newShortTermTrimBank1(((ShortTermTrimBank1) command).getNumberResult());
			logger.info("Processed ShortTermTrimBank1: "+ command.toString());
		}
		
		else if (command instanceof LongTermTrimBank1) {
			this.collector.newLongTermTrimBank1(((LongTermTrimBank1) command).getNumberResult());
			logger.info("Processed LongTermTrimBank1: "+ command.toString());
		}
	}
	

	private boolean isNoDataCommand(CommonCommand command) {
		if (command.getRawData() != null && (command.getRawData().equals("NODATA") ||
				command.getRawData().equals(""))) return true;
		
		if (command.getRawData() == null) return true;
		
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
		if (track == null) {
			if (++trackCreationTries < MAX_CREATION_TRIES) {
				createNewTrackIfNecessary();
			} else {
				logger.warn("Tried "+trackCreationTries +" times to resolve the correct track. Permanentely failing.");
			}
			
			if (track == null) return;
		}
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
		try {
			track.addMeasurement(measurement);
			logger.info(String.format("Add new measurement to track '%d': %s", track.getId(), measurement.toString()));
		} catch (TrackAlreadyFinishedException e) {
			logger.warn(e.getMessage(), e);
		}
		
	}
	
	/**
	 * This method determines whether it is necessary to create a new track or
	 * of the current/last used track should be reused
	 */
	private void createNewTrackIfNecessary() {
		DbAdapter dbAdapter = DbAdapterImpl.instance();
		// if track is null, create a new one or take the last one from the
		// database

		if (dbAdapter == null) {
			return;
		}
		
		Track lastUsedTrack;
		if (track == null) {
			lastUsedTrack = dbAdapter.getLastUsedTrack();
		}
		else {
			lastUsedTrack = track;
		}
		
		logger.info("createNewTrackIfNecessary: last? " + (lastUsedTrack == null ? "null" : lastUsedTrack.toString()));

		// New track if last measurement is more than 60 minutes
		// ago

		if (lastUsedTrack != null && lastUsedTrack.getStatus() != TrackStatus.FINISHED &&
				lastUsedTrack.getLastMeasurement() != null) {
			
			if ((System.currentTimeMillis() - lastUsedTrack
					.getLastMeasurement().getTime()) > MAX_TIME_BETWEEN_MEASUREMENTS) {
				logger.info(String.format("Create a new track: last measurement is more than %d mins ago",
						(int) (MAX_TIME_BETWEEN_MEASUREMENTS / 1000 / 60)));
				track = dbAdapter.createNewTrack();
			}

			// new track if last position is significantly different
			// from the current position (more than 3 km)
			else if (location == null || Util.getDistance(lastUsedTrack.getLastMeasurement().getLatitude(),lastUsedTrack.getLastMeasurement().getLongitude(),
					location.getLatitude(), location.getLongitude()) > MAX_DISTANCE_BETWEEN_MEASUREMENTS) {
				logger.info(String.format("Create a new track: last measurement's position is more than %f km away",
						MAX_DISTANCE_BETWEEN_MEASUREMENTS));
				track = dbAdapter.createNewTrack();
			}

			// TODO: New track if user clicks on create new track button

			// TODO: new track if VIN changed

			else {
				logger.info("Append to the last track: last measurement is close enough in space/time");
				track = lastUsedTrack;
			}
			
		}
		else {
			logger.info(String.format("Creating new Track. Last was null? %b; Last status was: %s; Last measurement: %s",
					lastUsedTrack == null,
					lastUsedTrack == null ? "n/a" : lastUsedTrack.getStatus().toString(),
					lastUsedTrack == null ? "n/a" : lastUsedTrack.getLastMeasurement()));
			track = dbAdapter.createNewTrack();
		}
			
		logger.info(String.format("Using Track: %s / id: %d", track.getName(), track.getId()));
		if (trackMetadata != null) {
			this.track.updateMetadata(trackMetadata);
		}
	}

	@Override
	public void receiveEvent(LocationEvent event) {
		this.location = event.getPayload();
		this.collector.newLocation(event.getPayload());
	}

	public void shutdown() {
		EventBus.getInstance().unregisterListener(this);
		EventBus.getInstance().unregisterListener(dopListener);
		
		synchronized (CommandListener.class) {
			if (!this.shutdownCompleted) {
				instanceCount--;
				this.shutdownCompleted = true;
			}
			
		}
	}

	@Override
	public void onConnected(String deviceName) {
		trackMetadata = new TrackMetadata();
		trackMetadata.putEntry(TrackMetadata.OBD_DEVICE, deviceName);
		
		if (track != null) {
			this.track.updateMetadata(trackMetadata);
		}
	}

	
}
