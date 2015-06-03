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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.envirocar.app.Injector;
import org.envirocar.app.activity.SettingsActivity;
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
import org.envirocar.app.event.RPMEvent;
import org.envirocar.app.event.SpeedEvent;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.MeasurementSerializationException;
import org.envirocar.app.storage.TrackAlreadyFinishedException;
import org.envirocar.app.storage.TrackMetadata;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link EventBus}.
 *
 * @author matthes rieke
 *
 */
public class CommandListener implements Listener, MeasurementListener {

	private static final Logger logger = Logger.getLogger(CommandListener.class);

	private Collector collector;

	private GpsDOPEventListener dopListener;

	private TrackMetadata obdDeviceMetadata;

	private boolean shutdownCompleted = false;

	private static int instanceCount;
	private ExecutorService inserter = new ThreadPoolExecutor(1, 1, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
			new RejectedExecutionHandler() {
				@Override
				public void rejectedExecution(Runnable r,
						ThreadPoolExecutor executor) {
					logger.warn(String.format("Execution rejected: %s / %s", r.toString(), executor.toString()));
				}

	});

    // Injected variables
    @Inject
    protected Context mContext;
    @Inject
    protected CarManager mCarManager;
    @Inject
    protected DbAdapter mDBAdapter;


    public CommandListener(Context context){
        ((Injector) context).injectObjects(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                (context.getApplicationContext());

        String samplingRate = sharedPreferences.getString(SettingsActivity.SAMPLING_RATE, null);

        int val;
        if  (samplingRate != null) {
            try {
                val = Integer.parseInt(samplingRate) * 1000;
            }
            catch (NumberFormatException e) {
                val = Collector.DEFAULT_SAMPLING_RATE_DELTA;
            }
        }
        else {
            val = Collector.DEFAULT_SAMPLING_RATE_DELTA;
        }

        this.collector = new Collector(this, mCarManager.getCar(), val);
        dopListener = new GpsDOPEventListener() {
            @Override
            public void receiveEvent(GpsDOPEvent event) {
                GpsDOP dop = event.getPayload();
                collector.newDop(dop);
            }
        };
        EventBus.getInstance().registerListener(dopListener);
        EventBus.getInstance().registerListener(this.collector);

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
//				logger.info("Processed Speed Response: "+speedMeasurement +" time: "+command.getResultTime());
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
//				logger.info("Processed RPM Response: "+rpmMeasurement +" time: "+command.getResultTime());
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
//				logger.info("Processed IAP Response: "+intakePressureMeasurement +" time: "+command.getResultTime());
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
//				logger.info("Processed IAT Response: "+intakeTemperatureMeasurement +" time: "+command.getResultTime());
			} catch (NumberFormatException e) {
				logger.warn("Intake Temperature parse exception", e);
			}
		}

		else if (command instanceof MAF) {
			float mafMeasurement = (Float) numberCommand.getNumberResult();
			this.collector.newMAF(mafMeasurement);
//			logger.info("Processed MAF Response: "+mafMeasurement +" time: "+command.getResultTime());
		}


		else if (command instanceof TPS) {
			int tps = (Integer) numberCommand.getNumberResult();
			this.collector.newTPS(tps);
//			logger.info("Processed TPS Response: "+tps +" time: "+command.getResultTime());
		}

		else if (command instanceof EngineLoad) {
			double load = (Float) numberCommand.getNumberResult();
			this.collector.newEngineLoad(load);
//			logger.info("Processed EngineLoad Response: "+load +" time: "+command.getResultTime());
		}

		else if (command instanceof FuelSystemStatus) {
			boolean loop = ((FuelSystemStatus) command).isInClosedLoop();
			int status = ((FuelSystemStatus) command).getStatus();
			this.collector.newFuelSystemStatus(loop, status);
//			logger.info("Processed FuelSystemStatus Response: Closed? "+loop +" Status: "+ status +"; time: "+command.getResultTime());
		}

		else if (command instanceof O2LambdaProbe) {
			this.collector.newLambdaProbeValue((O2LambdaProbe) command);
//			logger.info("Processed O2LambdaProbe Response: "+ command.toString());
		}

		else if (command instanceof ShortTermTrimBank1) {
			this.collector.newShortTermTrimBank1(((ShortTermTrimBank1) command).getNumberResult());
//			logger.info("Processed ShortTermTrimBank1: "+ command.toString());
		}

		else if (command instanceof LongTermTrimBank1) {
			this.collector.newLongTermTrimBank1(((LongTermTrimBank1) command).getNumberResult());
//			logger.info("Processed LongTermTrimBank1: "+ command.toString());
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
	public void insertMeasurement(final Measurement measurement) {
		logger.info(String.format("Invoking insertion from Thread %s and CommandListener %s: %s",
				Thread.currentThread().getId(), System.identityHashCode(CommandListener.this), measurement));
		this.inserter.submit(new Runnable() {
			@Override
			public void run() {
				try {
					mDBAdapter.insertNewMeasurement(measurement);
				} catch (TrackAlreadyFinishedException e) {
					logger.warn(e.getMessage(), e);
				} catch (MeasurementSerializationException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		});
	}

	public void shutdown() {
		logger.info("shutting down CommandListener. Hash: "+ System.identityHashCode(this));

		EventBus.getInstance().unregisterListener(dopListener);
		EventBus.getInstance().registerListener(this.collector);

		this.inserter.shutdown();

		synchronized (CommandListener.class) {
			if (!this.shutdownCompleted) {
				instanceCount--;
				this.shutdownCompleted = true;
			}

		}
	}

	@Override
	public void onConnected(String deviceName) {
		obdDeviceMetadata = new TrackMetadata();
		obdDeviceMetadata.putEntry(TrackMetadata.OBD_DEVICE, deviceName);

		mDBAdapter.setConnectedOBDDevice(obdDeviceMetadata);
	}


}
