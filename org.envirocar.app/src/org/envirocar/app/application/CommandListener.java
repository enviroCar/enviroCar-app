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

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.event.CO2Event;
import org.envirocar.app.event.ConsumptionEvent;
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
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.AndroidUtil;
import org.envirocar.app.views.Utils;

import android.location.Location;
import android.net.ParseException;
import android.widget.Toast;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link EventBus}.
 * 
 * @author matthes rieke
 *
 */
public class CommandListener implements Listener, LocationEventListener {
	
	private static final Logger logger = Logger.getLogger(CommandListener.class);

	// Track properties

	private Track track;
	private String trackDescription = "Description of the track";
	
	// Measurement values
	
	private Location location;
	private int speedMeasurement = 0;
	private double co2Measurement = 0.0;
	private double mafMeasurement;
	private double calculatedMafMeasurement = 0;
	private int rpmMeasurement = 0;
	private int intakeTemperatureMeasurement = 0;
	private int intakePressureMeasurement = 0;
	private Measurement measurement = null;
	private long lastInsertTime = 0;

	private Car car;

	private DbAdapter dbAdapterLocal;
	
	public CommandListener(Car car, DbAdapter dbAdapterLocal) {
		this.car = car;
		this.dbAdapterLocal = dbAdapterLocal;
		EventBus.getInstance().registerListener(this);
	}
	
	public void receiveUpdate(CommonCommand job) {
		logger.debug("update received");
		// Get the name and the result of the Command

		String commandName = job.getCommandName();
		String commandResult = job.getResult();
		logger.debug(commandName + " " + commandResult);
		if (commandResult.equals("NODATA"))
			return;

		/*
		 * Check which measurent is returned and save the value in the
		 * previously created measurement
		 */

		// Speed

		if (commandName.equals("Vehicle Speed")) {

			try {
				speedMeasurement = Integer.valueOf(commandResult);
				EventBus.getInstance().fireEvent(new SpeedEvent(speedMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("speed parse exception", e);
			}
		}
		
		//RPM
		
		else if (commandName.equals("Engine RPM")) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				rpmMeasurement = Integer.valueOf(commandResult);
				EventBus.getInstance().fireEvent(new RPMEvent(rpmMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("rpm parse exception", e);
			}
		}

		//IntakePressure
		
		else if (commandName.equals("Intake Manifold Pressure")) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				intakePressureMeasurement = Integer.valueOf(commandResult);
				EventBus.getInstance().fireEvent(new IntakePressureEvent(intakePressureMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("Intake Pressure parse exception", e);
			}
		}
		
		//IntakeTemperature
		
		else if (commandName.equals("Air Intake Temperature")) {
			// TextView speedTextView = (TextView)
			// findViewById(R.id.spd_text);
			// speedTextView.setText(commandResult + " km/h");

			try {
				intakeTemperatureMeasurement = Integer.valueOf(commandResult);
				EventBus.getInstance().fireEvent(new IntakeTemperatureEvent(intakeTemperatureMeasurement));
			} catch (NumberFormatException e) {
				logger.warn("Intake Temperature parse exception", e);
			}
		}
						
		//calculate alternative maf from iat, map, rpm
		double imap = rpmMeasurement * intakePressureMeasurement / (intakeTemperatureMeasurement+273);
		//VE = 85 in most modern cars
		double calculatedMaf = imap / 120.0d * 85.0d/100.0d * car.getEngineDisplacement() * 28.97 / 8.317;	
		calculatedMafMeasurement = calculatedMaf;
		
		logger.info("calculatedMaf: "+calculatedMaf+"; engineDisplacement: "+car.getEngineDisplacement());
		
		// MAF

		if (commandName.equals("Mass Air Flow")) {
			String maf = commandResult;

			try {
				NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
				Number number;
				number = format.parse(maf);
				mafMeasurement = number.doubleValue();

				// Dashboard Co2 current value preparation

				double consumption = 0.0;

				double tmpMaf = 0.0;
				if (mafMeasurement > 0.0) {
					tmpMaf = mafMeasurement;
				} else {
					tmpMaf = calculatedMafMeasurement;
				}
				
				if (car.getFuelType() == FuelType.GASOLINE) {
					consumption = (tmpMaf / 14.7) / 747;
					// Change to l/h
					consumption=consumption*3600;
					co2Measurement = consumption * 2.35; //kg/h
				} else if (car.getFuelType() == FuelType.DIESEL) {
					consumption = (tmpMaf / 14.5) / 832;
					// Change to l/h
					consumption=consumption*3600;
					co2Measurement = consumption * 2.65; //kg/h
				}

				logger.info("co2: "+ co2Measurement+"");
				EventBus.getInstance().fireEvent(new ConsumptionEvent(consumption));
				EventBus.getInstance().fireEvent(new CO2Event(co2Measurement));
			} catch (ParseException e) {
				logger.warn("parse exception maf", e);
			} catch (java.text.ParseException e) {
				logger.warn("parse exception maf", e);
			}
		}

		// Update and insert the measurement

		updateMeasurement();
	}
	
	/**
	 * Helper Command that updates the current measurement with the last
	 * measurement data and inserts it into the database if the measurements is
	 * young enough
	 */
	public void updateMeasurement() {

		// Create track new measurement if necessary

		if (measurement == null) {
			measurement = new Measurement(location.getLatitude(), location.getLongitude());
		}

		// Insert the values if the measurement (with the coordinates) is young
		// enough (5000ms) or create new one if it is too old

		if (measurement.getLatitude() != 0.0 && measurement.getLongitude() != 0.0) {

			if (Math.abs(measurement.getMeasurementTime() - System.currentTimeMillis()) > 5000) {
				measurement = new Measurement(location.getLatitude(), location.getLongitude());
			}

			measurement.setSpeed(speedMeasurement);
			measurement.setMaf(mafMeasurement);	
			measurement.setCalculatedMaf(calculatedMafMeasurement);
			measurement.setRpm(rpmMeasurement);
			measurement.setIntakePressure(intakePressureMeasurement);
			measurement.setIntakeTemperature(intakeTemperatureMeasurement);
			insertMeasurement(measurement);
		} else {
			logger.warn("Position by GPS isn't working correct");
		}
	}

	/**
	 * Helper method to insert track measurement into the database (ensures that
	 * track measurement is only stored every 5 seconds and not faster...)
	 * 
	 * @param insertMeasurement
	 *            The measurement you want to insert
	 */
	private void insertMeasurement(Measurement insertMeasurement) {

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

		if (Math.abs(lastInsertTime - insertMeasurement.getMeasurementTime()) > 5000) {

			lastInsertTime = insertMeasurement.getMeasurementTime();

			track.addMeasurement(insertMeasurement);

			logger.info("Add new measurement to track: " + insertMeasurement.toString());

			AndroidUtil.getInstance().makeTextToast("Calculated Mass Air Flow" + measurement.getCalculatedMaf(),
					Toast.LENGTH_SHORT);

		}

	}
	
	/**
	 * This method determines whether it is necessary to create a new track or
	 * of the current/last used track should be reused
	 */
	@Override
	public void createNewTrackIfNecessary() {

		// setting undefined, will hopefully prevent correct uploading.
		// but this shouldn't be possible to record tracks without these values
		String fuelType = car.getFuelType().toString();
		String carManufacturer = car.getManufacturer();
		String carModel = car.getModel();
		String sensorId = car.getId();

		// if track is null, create a new one or take the last one from the
		// database
		
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH)+1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		String date = String.valueOf(year) + "-" + String.valueOf(month) + "-" + String.valueOf(day);

		if (track == null) {

			logger.info("The track was null");

			Track lastUsedTrack;

			try {
				lastUsedTrack = dbAdapterLocal.getLastUsedTrack();

				try {

					// New track if last measurement is more than 60 minutes
					// ago

					if ((System.currentTimeMillis() - lastUsedTrack
							.getLastMeasurement().getMeasurementTime()) > 3600000) {
						logger.info("I create a new track because the last measurement is more than 60 mins ago");
						track = new Track("123456", fuelType, carManufacturer,
								carModel, sensorId, dbAdapterLocal);
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						track.commitTrackToDatabase();
						return;
					}

					// new track if last position is significantly different
					// from the current position (more than 3 km)
					if (Utils.getDistance(lastUsedTrack.getLastMeasurement().getLatitude(),lastUsedTrack.getLastMeasurement().getLongitude(),
							location.getLatitude(), location.getLongitude()) > 3.0) {
						logger.info("The last measurement's position is more than 3 km away. I will create a new track");
						track = new Track("123456", fuelType, carManufacturer,
								carModel, sensorId, dbAdapterLocal); 
						track.setName("Track " + date);
						track.setDescription(trackDescription);
						track.commitTrackToDatabase();
						return;

					}

					// TODO: New track if user clicks on create new track button

					// TODO: new track if VIN changed

					else {
						logger.info("I will append to the last track because that still makes sense");
						track = lastUsedTrack;
						return;
					}

				} catch (MeasurementsException e) {
					logger.warn("The last track contains no measurements. I will delete it and create a new one.");
					dbAdapterLocal.deleteTrack(lastUsedTrack.getId());
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
				}

			} catch (TracksException e) {
				logger.warn("There was no track in the database so I created a new one");
				track = new Track("123456", fuelType, carManufacturer,
						carModel, sensorId, dbAdapterLocal); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				track.commitTrackToDatabase();
			}

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
						.getLastMeasurement().getMeasurementTime()) > 3600000) {
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal);
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
					logger.info("I create a new track because the last measurement is more than 60 mins ago");
					return;
				}
				// TODO: New track if user clicks on create new track button

				// new track if last position is significantly different from
				// the
				// current position (more than 3 km)

				if (Utils.getDistance(currentTrack.getLastMeasurement().getLatitude(),currentTrack.getLastMeasurement().getLongitude(),
						location.getLatitude(), location.getLongitude()) > 3.0) {
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal); 
					track.setName("Track " + date);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
					logger.info("The last measurement's position is more than 3 km away. I will create a new track");
					return;

				}

				// TODO: new track if VIN changed

				else {
					logger.info("I will append to the last track because that still makes sense");
					return;
				}

			} catch (MeasurementsException e) {
				logger.warn("The last track contains no measurements. I will delete it and create a new one.");
				dbAdapterLocal.deleteTrack(currentTrack.getId());
				track = new Track("123456", fuelType, carManufacturer,
						carModel, sensorId, dbAdapterLocal); 
				track.setName("Track " + date);
				track.setDescription(trackDescription);
				track.commitTrackToDatabase();
			}
		}
	}


	@Override
	public void resetTrack() {
		this.track = null;
	}

	@Override
	public void receiveEvent(LocationEvent event) {
		this.location = event.getPayload();
	}

	
}
