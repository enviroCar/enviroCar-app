/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app;

import android.content.Context;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.gps.GpsDOPEvent;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackAlreadyFinishedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.obd.Listener;
import org.envirocar.obd.MeasurementListener;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.EngineLoadResponse;
import org.envirocar.obd.commands.response.entity.EngineRPMResponse;
import org.envirocar.obd.commands.response.entity.FuelSystemStatusResponse;
import org.envirocar.obd.commands.response.entity.IntakeAirTemperatureResponse;
import org.envirocar.obd.commands.response.entity.IntakeManifoldAbsolutePressureResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeCurrentResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.commands.response.entity.LongTermFuelTrimResponse;
import org.envirocar.obd.commands.response.entity.MAFResponse;
import org.envirocar.obd.commands.response.entity.ShortTermFuelTrimResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.envirocar.obd.commands.response.entity.ThrottlePositionResponse;
import org.envirocar.obd.events.IntakePreasureUpdateEvent;
import org.envirocar.obd.events.IntakeTemperatureUpdateEvent;
import org.envirocar.obd.events.RPMUpdateEvent;
import org.envirocar.obd.events.SpeedUpdateEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Standalone listener class for OBDII commands. It provides all
 * received processed commands through the {@link Bus}.
 *
 * @author matthes rieke
 */
public class CommandListener implements Listener, MeasurementListener {
    // TODO change listener stuff

    private static final Logger logger = Logger.getLogger(CommandListener.class);

    private Collector collector;
    private TrackMetadata obdDeviceMetadata;

    private boolean shutdownCompleted = false;

    private static int instanceCount;
    private ExecutorService inserter = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors
            .defaultThreadFactory(),
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r,
                                              ThreadPoolExecutor executor) {
                    logger.warn(String.format("Execution rejected: %s / %s", r.toString(),
                            executor.toString()));
                }

            });

    // Injected variables
    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected DbAdapter mDBAdapter;


    public CommandListener(Context context) {
        // First, inject all annotated fields.
        ((Injector) context).injectObjects(this);

        // then register on the bus.
        this.mBus.register(this);

        String samplingRate = PreferenceManager.getDefaultSharedPreferences
                (context.getApplicationContext()).getString(PreferenceConstants.SAMPLING_RATE, null);

        int val;
        if (samplingRate != null) {
            try {
                val = Integer.parseInt(samplingRate) * 1000;
            } catch (NumberFormatException e) {
                val = Collector.DEFAULT_SAMPLING_RATE_DELTA;
            }
        } else {
            val = Collector.DEFAULT_SAMPLING_RATE_DELTA;
        }

        this.collector = new Collector(mContext, this, mCarManager.getCar(), val);

//        EventBus.getInstance().registerListener(this.collector);

        synchronized (CommandListener.class) {
            instanceCount++;
            logger.debug("Initialized. Hash: " + System.identityHashCode(this) + "; active " +
                    "instances: " + instanceCount);
        }
    }

    @Subscribe
    public void onReceiveGpsDOPEvent(GpsDOPEvent event) {
        logger.info(String.format("Received event: %s", event.toString()));
        if (collector != null)
            collector.newDop(event.mDOP);
    }

    public void receiveUpdate(DataResponse command) {

		/*
         * Check which measurment is returned and save the value in the
		 * previously created measurement
		 */

        // Speed

        if (command instanceof SpeedResponse) {
            try {
                Integer speedMeasurement = ((SpeedResponse) command).getValue().intValue();
                this.collector.newSpeed(speedMeasurement);
                mBus.post(new SpeedUpdateEvent(speedMeasurement));
                logger.info("Processed Speed Response: " + speedMeasurement + " time: " + command
                        .getTimestamp());
            } catch (NumberFormatException e) {
                logger.warn("speed parse exception", e);
            }
        }

        //RPM

        else if (command instanceof EngineRPMResponse) {

            try {
                Integer rpmMeasurement = ((EngineRPMResponse) command).getValue().intValue();
                this.collector.newRPM(rpmMeasurement);
                mBus.post(new RPMUpdateEvent(rpmMeasurement));
//				logger.info("Processed RPM Response: "+rpmMeasurement +" time: "+command
// .getResultTime());
            } catch (NumberFormatException e) {
                logger.warn("rpm parse exception", e);
            }
        }

        //IntakePressure

        else if (command instanceof IntakeManifoldAbsolutePressureResponse) {
            try {
                Integer intakePressureMeasurement = ((IntakeManifoldAbsolutePressureResponse) command).getValue().intValue();
                this.collector.newIntakePressure(intakePressureMeasurement);
                mBus.post(new IntakePreasureUpdateEvent(intakePressureMeasurement));
//				logger.info("Processed IAP Response: "+intakePressureMeasurement +" time:
// "+command.getResultTime());
            } catch (NumberFormatException e) {
                logger.warn("Intake Pressure parse exception", e);
            }
        }

        //IntakeTemperature

        else if (command instanceof IntakeAirTemperatureResponse) {
            try {
                Integer intakeTemperatureMeasurement = ((IntakeAirTemperatureResponse) command).getValue().intValue();
                this.collector.newIntakeTemperature(intakeTemperatureMeasurement);
                this.mBus.post(new IntakeTemperatureUpdateEvent(intakeTemperatureMeasurement));
//				logger.info("Processed IAT Response: "+intakeTemperatureMeasurement +" time:
// "+command.getResultTime());
            } catch (NumberFormatException e) {
                logger.warn("Intake Temperature parse exception", e);
            }
        } else if (command instanceof MAFResponse) {
            float mafMeasurement = ((MAFResponse) command).getValue().floatValue();
            this.collector.newMAF(mafMeasurement);
//			logger.info("Processed MAF Response: "+mafMeasurement +" time: "+command.getResultTime
// ());
        } else if (command instanceof ThrottlePositionResponse) {
            int tps = ((ThrottlePositionResponse) command).getValue().intValue();
            this.collector.newTPS(tps);
//			logger.info("Processed TPS Response: "+tps +" time: "+command.getResultTime());
        } else if (command instanceof EngineLoadResponse) {
            double load = ((EngineLoadResponse) command).getValue().doubleValue();
            this.collector.newEngineLoad(load);
//			logger.info("Processed EngineLoad Response: "+load +" time: "+command.getResultTime());
        } else if (command instanceof FuelSystemStatusResponse) {
            boolean loop = ((FuelSystemStatusResponse) command).isInClosedLoop();
            int status = ((FuelSystemStatusResponse) command).getStatus();
            this.collector.newFuelSystemStatus(loop, status);
//			logger.info("Processed FuelSystemStatus Response: Closed? "+loop +" Status: "+ status
// +"; time: "+command.getResultTime());
        } else if (command instanceof LambdaProbeCurrentResponse) {
            this.collector.newLambdaProbeValue((LambdaProbeCurrentResponse) command);
//			logger.info("Processed O2LambdaProbe Response: "+ command.toString());
        } else if (command instanceof LambdaProbeVoltageResponse) {
            this.collector.newLambdaProbeValue((LambdaProbeVoltageResponse) command);
//			logger.info("Processed O2LambdaProbe Response: "+ command.toString());
        } else if (command instanceof ShortTermFuelTrimResponse) {
            this.collector.newShortTermTrimBank1(((ShortTermFuelTrimResponse) command).getValue());
//			logger.info("Processed ShortTermTrimBank1: "+ command.toString());
        } else if (command instanceof LongTermFuelTrimResponse) {
            this.collector.newLongTermTrimBank1(((LongTermFuelTrimResponse) command).getValue());
//			logger.info("Processed LongTermTrimBank1: "+ command.toString());
        }
    }


    /**
     * Helper method to insert track measurement into the database (ensures that
     * track measurement is only stored every 5 seconds and not faster...)
     *
     * @param measurement The measurement you want to insert
     */
    public void insertMeasurement(final Measurement measurement) {
        logger.warn(String.format("Invoking insertion from Thread %s and CommandListener %s: %s",
                Thread.currentThread().getId(), System.identityHashCode(CommandListener.this),
                measurement));
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
        if(!shutdownCompleted)
            return;

        // Unregister from the eventbus.
        mBus.unregister(this);

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
