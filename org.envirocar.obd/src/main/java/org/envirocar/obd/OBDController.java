/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.obd;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.adapter.AposW3Adapter;
import org.envirocar.obd.adapter.CarTrendAdapter;
import org.envirocar.obd.adapter.ELM327Adapter;
import org.envirocar.obd.adapter.OBDAdapter;
import org.envirocar.obd.adapter.OBDLinkAdapter;
import org.envirocar.obd.adapter.UniCarScanAdapter;
import org.envirocar.obd.adapter.async.DriveDeckSportAdapter;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.envirocar.obd.events.RPMUpdateEvent;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.obd.exception.AllAdaptersFailedException;
import org.envirocar.obd.exception.EngineNotRunningException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * this is the main class for interacting with a OBD-II adapter.
 * It takes {@link InputStream} and {@link OutputStream} objects
 * to do the actual raw communication. The {@link ConnectionListener} will get informed on
 * certain changes in the connection state.
 *
 * @author matthes rieke
 */
public class OBDController {
    private static final Logger LOG = Logger.getLogger(OBDController.class);
    public static final long MAX_NODATA_TIME = 10000;

    private Disposable initSubscription;
    private Disposable dataSubscription;

    private Queue<OBDAdapter> adapterCandidates = new ArrayDeque<>();
    private OBDAdapter obdAdapter;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ConnectionListener connectionListener;
    private String deviceName;
    private boolean userRequestedStop = false;
    private Bus eventBus;
    private Scheduler.Worker eventBusWorker;

    /**
     * Default Constructor.
     *
     * @param bluetoothSocketWrapper
     * @param cl
     * @param bus
     */
    public OBDController(BluetoothSocketWrapper bluetoothSocketWrapper, ConnectionListener cl,
                         Bus bus) throws IOException {
        this(bluetoothSocketWrapper.getInputStream(),
                bluetoothSocketWrapper.getOutputStream(),
                bluetoothSocketWrapper.getRemoteDeviceName(),
                cl, bus);
    }

    /**
     * Init the OBD control layer with the streams and listeners to be used.
     *
     * @param in  the inputStream of the connection
     * @param out the outputStream of the connection
     * @param cl  the connection listener which receives connection state changes
     */
    public OBDController(InputStream in, OutputStream out,
                         String deviceName, ConnectionListener cl, Bus bus) {
        this.inputStream = Preconditions.checkNotNull(in);
        this.outputStream = Preconditions.checkNotNull(out);
        this.connectionListener = Preconditions.checkNotNull(cl);
        this.deviceName = Preconditions.checkNotNull(deviceName);

        setupAdapterCandidates();
        startPreferredAdapter();

        this.eventBus = bus;
        if (this.eventBus != null) {
            this.eventBusWorker = Schedulers.io().createWorker();
        }
    }

    /**
     * setup the list of available Adapter implementations
     */
    private void setupAdapterCandidates() {
        adapterCandidates.clear();
        adapterCandidates.offer(new ELM327Adapter());
        adapterCandidates.offer(new UniCarScanAdapter());
        adapterCandidates.offer(new OBDLinkAdapter());
        adapterCandidates.offer(new CarTrendAdapter());
        adapterCandidates.offer(new AposW3Adapter());
        adapterCandidates.offer(new DriveDeckSportAdapter());
    }

    /**
     * start the preferred adapter, determined by the device name
     */
    private void startPreferredAdapter() {
        for (OBDAdapter ac : adapterCandidates) {
            if (ac.supportsDevice(this.deviceName)) {
                this.obdAdapter = ac;
                break;
            }
        }

        if (this.obdAdapter == null) {
            //poll the first instead
            this.obdAdapter = adapterCandidates.poll();
        } else {
            //remove the preferred from the queue so it is not used again
            this.adapterCandidates.remove(this.obdAdapter);
        }

        LOG.info("Using " + this.obdAdapter.getClass().getSimpleName() + " connector as the " +
                "preferred adapter for device with name "+ this.deviceName +".");
        startInitialization(false);
    }

    /**
     * select the next adapter candidates from the list of implementations
     *
     * @throws AllAdaptersFailedException if the list has reached its end
     */
    private void selectNextAdapter() throws AllAdaptersFailedException {
        this.obdAdapter = adapterCandidates.poll();

        if (this.obdAdapter == null) {
            throw new AllAdaptersFailedException("All candidate adapters failed");
        }
    }

    /**
     * start the init method of the adapter. This is used
     * to bootstrap and verify the connection of the adapter
     * with the ECU.
     * <p>
     * The init times out after a pre-defined period.
     */
    private void startInitialization(boolean alreadyTried) {
        LOG.info("startInitialization()");

        // start the observable and subscribe to it
        this.initSubscription = this.obdAdapter.initialize(this.inputStream, this.outputStream)
                .subscribeOn(Schedulers.io())
                .observeOn(OBDSchedulers.scheduler())
                .timeout(this.obdAdapter.getExpectedInitPeriod(), TimeUnit.MILLISECONDS)
                .subscribeWith(getInitSubscriber(alreadyTried));
    }

    private DisposableObserver<Boolean> getInitSubscriber(boolean alreadyTried) {
        return new DisposableObserver<Boolean>() {

            @Override
            public void onError(Throwable e) {
                LOG.warn("Adapter failed: " + obdAdapter.getClass().getSimpleName(), e);
                if (e instanceof EngineNotRunningException){
                    connectionListener.onEngineNotRunning();
                    return;
                }

                try {
                    LOG.info("State message is: "+obdAdapter.getStateMessage());
                }
                catch (Exception ex) {
                    LOG.warn("Could not log state message", ex);
                }


                try {

                    if (obdAdapter.hasCertifiedConnection()) {
                        if (!alreadyTried) {
                            // one retry if it was verified!
                            startInitialization(true);
                        } else {
                            throw new AllAdaptersFailedException(
                                    "Adapter verified a connection but could not establishe data: "
                                            + obdAdapter.getClass().getSimpleName());
                        }
                    } else {
                        selectNextAdapter();

                        // try the selected adapter
                        startInitialization(false);
                    }

                } catch (AllAdaptersFailedException e1) {
                    LOG.warn("All Adapters failed", e1);
                    connectionListener.onAllAdaptersFailed();
                    //TODO implement equivalent notification method:
                    //dataListener.shutdown();
                }
            }

            @Override
            public void onComplete() {
                LOG.info("Connecting has been initialized!");
            }

            @Override
            public void onNext(Boolean b) {
                LOG.info("Connection verified - starting data collection");
                try {
                    LOG.info("State message from adapter: "+obdAdapter.getStateMessage());
                }
                catch (Exception ex) {
                    LOG.warn("Could not log state message", ex);
                }

                //unsubscribe, otherwise we will get a timeout
                this.onComplete();

                startCollectingData();
                //TODO implement equivalent notification method:
                //dataListener.onConnected(deviceName);
            }

        };
    }

    /**
     * start the actual collection of data.
     * <p>
     * the collection times out after a pre-defined period when no
     * new data has arrived.
     */
    private void startCollectingData() {
        LOG.info("OBDController.startCollectingData()");

        // start the observable with a timeout
        this.dataSubscription = this.obdAdapter.observe()
                .subscribeOn(OBDSchedulers.scheduler())
                .observeOn(OBDSchedulers.scheduler())
                .timeout(MAX_NODATA_TIME, TimeUnit.MILLISECONDS)
                .subscribeWith(getCollectingDataSubscriber());

        //inform the listener about the successful conn
        this.connectionListener.onConnectionVerified();
    }

    private DisposableObserver<DataResponse> getCollectingDataSubscriber() {
        return new DisposableObserver<DataResponse>() {
            @Override
            protected void onStart() {
                LOG.info("OnStart()");
            }

            @Override
            public void onError(Throwable e) {
                LOG.warn("onError() received", e);

                // check if this is a demanded stop: still this can lead to any kind of Exception
                if (userRequestedStop) {
                    //TODO implement equivalent notification method:
                    //dataListener.shutdown();
                }

                connectionListener.onAllAdaptersFailed();

            }

            @Override
            public void onComplete() {
                LOG.info("onCompleted(): data collection");
                //TODO implement equivalent notification method:
                //dataListener.shutdown();
            }

            @Override
            public void onNext(DataResponse dataResponse) {
                pushToEventBus(dataResponse);
            }
        };
    }

    private void pushToEventBus(DataResponse dataResponse) {
        eventBusWorker.schedule(() -> {
            PropertyKeyEvent[] pkes = createEventsFromDataResponse(dataResponse);

            for (PropertyKeyEvent pke : pkes) {
                eventBus.post(pke);
            }

            PID pid = dataResponse.getPid();
            if (pid == PID.SPEED) {
                eventBus.post(new SpeedUpdateEvent(dataResponse.getValue().intValue()));
            } else if (pid == PID.RPM) {
                eventBus.post(new RPMUpdateEvent(dataResponse.getValue().intValue()));
            }
        });
    }

    protected PropertyKeyEvent[] createEventsFromDataResponse(DataResponse dataResponse) {
        PID pid = dataResponse.getPid();
        switch (pid) {
//            case FUEL_SYSTEM_STATUS:
            case CALCULATED_ENGINE_LOAD:
            case SHORT_TERM_FUEL_TRIM_BANK_1:
            case LONG_TERM_FUEL_TRIM_BANK_1:
            case FUEL_PRESSURE:
            case INTAKE_MAP:
            case RPM:
            case SPEED:
            case INTAKE_AIR_TEMP:
            case MAF:
            case TPS:
                return new PropertyKeyEvent[]{
                        new PropertyKeyEvent(PIDUtil.toPropertyKey(pid),
                                dataResponse.getValue(), dataResponse.getTimestamp())
                };
            case O2_LAMBDA_PROBE_1_VOLTAGE:
            case O2_LAMBDA_PROBE_2_VOLTAGE:
            case O2_LAMBDA_PROBE_3_VOLTAGE:
            case O2_LAMBDA_PROBE_4_VOLTAGE:
            case O2_LAMBDA_PROBE_5_VOLTAGE:
            case O2_LAMBDA_PROBE_6_VOLTAGE:
            case O2_LAMBDA_PROBE_7_VOLTAGE:
            case O2_LAMBDA_PROBE_8_VOLTAGE:
                return new PropertyKeyEvent[]{
                        new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER,
                                dataResponse.getCompositeValues()[0], dataResponse.getTimestamp()),
                        new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_VOLTAGE,
                                dataResponse.getCompositeValues()[1], dataResponse.getTimestamp())
                };
            case O2_LAMBDA_PROBE_1_CURRENT:
            case O2_LAMBDA_PROBE_2_CURRENT:
            case O2_LAMBDA_PROBE_3_CURRENT:
            case O2_LAMBDA_PROBE_4_CURRENT:
            case O2_LAMBDA_PROBE_5_CURRENT:
            case O2_LAMBDA_PROBE_6_CURRENT:
            case O2_LAMBDA_PROBE_7_CURRENT:
            case O2_LAMBDA_PROBE_8_CURRENT:
                return new PropertyKeyEvent[]{
                        new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_CURRENT_ER,
                                dataResponse.getCompositeValues()[0], dataResponse.getTimestamp()),
                        new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_CURRENT,
                                dataResponse.getCompositeValues()[1], dataResponse.getTimestamp())
                };
        }

        return new PropertyKeyEvent[0];
    }

    /**
     * Shutdown the controller. this removes all pending commands.
     * This object is no longer executable, a new instance has to
     * be created.
     * <p>
     * Only use this if the stop is from high-level (e.g. getUserStatistic request)
     * and NOT on any kind of exception
     */
    public void shutdown() {
        LOG.info("OBDController.shutdown()");

        /**
         * save that this is a stop on demand
         */
        userRequestedStop = true;

        if (this.initSubscription != null && !this.initSubscription.isDisposed()) {
            this.initSubscription.dispose();
        }
        if (this.dataSubscription != null && !this.dataSubscription.isDisposed()) {
            this.dataSubscription.dispose();
        }
    }
}
