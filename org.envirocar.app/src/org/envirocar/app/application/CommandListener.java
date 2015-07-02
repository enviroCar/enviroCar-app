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

import org.envirocar.app.bluetooth.obd.events.IntakePreasureUpdateEvent;
import org.envirocar.app.bluetooth.obd.events.IntakeTemperatureUpdateEvent;
import org.envirocar.app.bluetooth.obd.events.RPMUpdateEvent;
import org.envirocar.app.bluetooth.obd.events.SpeedUpdateEvent;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.bluetooth.obd.commands.CommonCommand;
import org.envirocar.app.bluetooth.obd.commands.EngineLoad;
import org.envirocar.app.bluetooth.obd.commands.FuelSystemStatus;
import org.envirocar.app.bluetooth.obd.commands.IntakePressure;
import org.envirocar.app.bluetooth.obd.commands.IntakeTemperature;
import org.envirocar.app.bluetooth.obd.commands.LongTermTrimBank1;
import org.envirocar.app.bluetooth.obd.commands.MAF;
import org.envirocar.app.bluetooth.obd.commands.NumberResultCommand;
import org.envirocar.app.bluetooth.obd.commands.O2LambdaProbe;
import org.envirocar.app.bluetooth.obd.commands.RPM;
import org.envirocar.app.bluetooth.obd.commands.ShortTermTrimBank1;
import org.envirocar.app.bluetooth.obd.commands.Speed;
import org.envirocar.app.bluetooth.obd.commands.TPS;
import org.envirocar.app.events.GpsDOPEvent;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.MeasurementSerializationException;
import org.envirocar.app.storage.TrackAlreadyFinishedException;
import org.envirocar.app.storage.TrackMetadata;

import android.content.Context;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link Bus}.
 *
 * @author matthes rieke
 *
 */
public class CommandListener implements Listener, MeasurementListener {
	// TODO change listener stuff

	private static final Logger logger = Logger.getLogger(CommandListener.class);

	private Collector collector;


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
	@InjectApplicationScope
	protected Context mContext;
	@Inject
	protected Bus mBus;
    @Inject
    protected CarManager mCarManager;
    @Inject
    protected DbAdapter mDBAdapter;


    public CommandListener(Context context){
        // First, inject all annotated fields.
        ((Injector) context).injectObjects(this);

        // then register on the bus.
        this.mBus.register(this);

        String samplingRate = PreferenceManager.getDefaultSharedPreferences
                (context.getApplicationContext()).getString(SettingsActivity.SAMPLING_RATE, null);

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

        this.collector = new Collector(mContext, this, mCarManager.getCar(), val);

//        EventBus.getInstance().registerListener(this.collector);

        synchronized (CommandListener.class) {
            instanceCount++;
            logger.debug("Initialized. Hash: "+System.identityHashCode(this) +"; active instances: "+instanceCount);
        }
    }

    @Subscribe
    public void onReceiveGpsDOPEvent(GpsDOPEvent event){
        logger.info(String.format("Received event: %s", event.toString()));
        collector.newDop(event.mDOP);
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
				mBus.post(new SpeedUpdateEvent(speedMeasurement));
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
                mBus.post(new RPMUpdateEvent(rpmMeasurement));
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
                mBus.post(new IntakePreasureUpdateEvent(intakePressureMeasurement));
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
                this.mBus.post(new IntakeTemperatureUpdateEvent(intakeTemperatureMeasurement));
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
		logger.info("shutting down CommandListener. Hash: " + System.identityHashCode(this));

//		EventBus.getInstance().registerListener(this.collector);

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
