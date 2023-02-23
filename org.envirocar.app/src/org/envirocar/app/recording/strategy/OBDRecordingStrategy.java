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
package org.envirocar.app.recording.strategy;

import android.app.Service;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackRecordingContinueEvent;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.recording.RecordingError;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.events.EngineNotRunningEvent;
import org.envirocar.app.recording.events.RecordingErrorEvent;
import org.envirocar.app.recording.notification.SpeechOutput;
import org.envirocar.app.recording.provider.LocationProvider;
import org.envirocar.app.recording.provider.TrackDatabaseSink;
import org.envirocar.app.recording.strategy.obd.OBDConnectionHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.consumption.ConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.consumption.LoadBasedEnergyConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.statistics.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.obd.ConnectionListener;
import org.envirocar.obd.OBDController;
import org.envirocar.obd.OBDSchedulers;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.commands.CampagneCommandProfile;
import org.envirocar.obd.commands.CycleCommandProfile;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.obd.exception.AllAdaptersFailedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class OBDRecordingStrategy implements RecordingStrategy {
    private static final Logger LOG = Logger.getLogger(OBDRecordingStrategy.class);
    protected static final int MAX_RECONNECT_COUNT = 2;
    private static final String RECORDING_ERROR_EXTRA_DATA_KEY = "GPS_ERROR_SECONDS";

    protected Context context;
    protected Bus eventBus;
    protected SpeechOutput speechOutput;
    protected BluetoothHandler bluetoothHandler;
    protected OBDConnectionHandler obdConnectionHandler;
    protected MeasurementProvider measurementProvider;
    protected TrackDatabaseSink trackDatabaseSink;
    protected LocationProvider locationProvider;
    protected CarPreferenceHandler carPreferenceHandler;

    //
    private CompositeDisposable disposables = new CompositeDisposable();
    private RecordingListener listener;
    private OBDConnectionRecognizer recognizer = new OBDConnectionRecognizer();

    // computation algorithms
    private ConsumptionAlgorithm consumptionAlgorithm;
    private CalculatedMAFWithStaticVolumetricEfficiency mafAlgorithm;
    private LoadBasedEnergyConsumptionAlgorithm energyConsumptionAlgorithm;

    private boolean isRecording = false;
    private boolean isTrackFinished = false;
    private Track track = null;
    private CycleCommandProfile cycleCommandProfile;
    private int gpsConnectionDuration = 60 * 2;

    /**
     * Constructor.
     */
    public OBDRecordingStrategy(
            Context context, Bus eventBus, SpeechOutput speechOutput, BluetoothHandler bluetoothHandler,
            OBDConnectionHandler obdConnectionHandler, MeasurementProvider measurementProvider,
            TrackDatabaseSink trackDatabaseSink, LocationProvider locationProvider,
            CarPreferenceHandler carPreferenceHandler) {
        this.context = context;
        this.eventBus = eventBus;
        this.speechOutput = speechOutput;
        this.bluetoothHandler = bluetoothHandler;
        this.obdConnectionHandler = obdConnectionHandler;
        this.measurementProvider = measurementProvider;
        this.trackDatabaseSink = trackDatabaseSink;
        this.locationProvider = locationProvider;
        this.carPreferenceHandler = carPreferenceHandler;

        // set the car specific properties.
        Car car = carPreferenceHandler.getCar();
        this.consumptionAlgorithm = ConsumptionAlgorithm.fromFuelType(car.getFuelType());
        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(car);
        this.energyConsumptionAlgorithm = new LoadBasedEnergyConsumptionAlgorithm(car.getFuelType());
        this.cycleCommandProfile = new CycleCommandProfile.Default();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        LOG.info("Destroying OBDRecordingStrategy");

        if (disposables != null) {
            disposables.clear();
        }
    }

    @Override
    public void startRecording(Service service, RecordingListener listener) {
        this.listener = listener;
        this.isTrackFinished = false;

        disposables.add(
                obdConnectionHandler.getOBDConnectionObservable(bluetoothHandler.getSelectedBluetoothDevice())
                        .compose(verifyConnection())
                        .compose(receiveMeasurements())
                        .compose(enhanceMeasurements())
                        .compose(trackDatabaseSink.storeInDatabase())
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(OBDSchedulers.scheduler())
                        .doOnDispose(() -> listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED))
                        .subscribeWith(initializeObserver()));

        disposables.add(
                locationProvider.startLocating()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.newThread())
                        .doOnDispose(() -> LOG.info("Location Provider has been disposed!"))
                        .subscribe(() -> LOG.info("Completed"), LOG::error));

        // subscribe for preference changes
        disposables.add(ApplicationSettings.getCampaignProfileObservable(context)
                .doOnNext(campaign -> this.cycleCommandProfile = getCycleCommandProfile(campaign))
                .subscribe());

        disposables.add(ApplicationSettings.getGPSConnectionDurationObservable(context)
                .doOnNext(duration -> this.gpsConnectionDuration = duration)
                .subscribe());
    }

    @Override
    public void stopRecording() {
        LOG.info("Stopping the track recording.");
        if (disposables != null) {
            disposables.clear();
        }

        try {
            eventBus.unregister(measurementProvider);
        } catch (Exception e) {
        }

        stopOBDConnectionRecognizer();
        if (isRecording) {
            speechOutput.doTextToSpeech("Track Recording Finished");
            isRecording = false;
        }
        listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
        notifyTrackFinished(track);
    }

    private DisposableObserver<Track> initializeObserver() {
        return new DisposableObserver<Track>() {

            @Override
            protected void onStart() {
                LOG.info("Starting the Bluetooth connection to the selected adapter");
                listener.onRecordingStateChanged(RecordingState.RECORDING_INIT);
                track = null;

                try {
                    recognizer = new OBDConnectionRecognizer();
                    eventBus.register(recognizer);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }

            @Override
            public void onNext(Track o) {
                LOG.info(String.format("Started new Track with ID=%s", o.getTrackID()));
                track = o;
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                stopOBDConnectionRecognizer();
            }

            @Override
            public void onComplete() {
                LOG.info("Finished the recording of the track.");
                listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                notifyTrackFinished(track);
                stopOBDConnectionRecognizer();
            }
        };
    }

    private void notifyTrackFinished(Track track) {
        if (!isTrackFinished && track != null) {
            this.listener.onTrackFinished(track);
            this.isTrackFinished = true;
        }
    }


    private ObservableTransformer<BluetoothSocketWrapper, BluetoothSocketWrapper> verifyConnection() {
        return upstream -> upstream.flatMap(socket -> Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;

            LOG.info(String.format("OBDConnectionService.onDeviceConntected(%s)", socket.getRemoteDeviceName()));

            try {
                OBDController controller = new OBDController(socket, this.cycleCommandProfile, new ConnectionListener() {
                    int reconnectCount = 0;

                    @Override
                    public void onConnectionVerified() {
                        if (emitter.isDisposed()) {
                            LOG.info("verifyConnection(): Emitter has been disposed before.");
                            return;
                        }
                        speechOutput.doTextToSpeech("Connection established.");
                        isRecording = true;
                        LOG.info("Connection verified. Starting to read measurements.");
                        listener.onRecordingStateChanged(RecordingState.RECORDING_RUNNING);
                        emitter.onNext(socket);
                    }

                    @Override
                    public void onEngineNotRunning() {
                        listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED);
                        eventBus.post(new EngineNotRunningEvent());
                    }

                    @Override
                    public void onAllAdaptersFailed() {
                        LOG.info("All adapters failed. Failed to connect to OBD adaper.");
                        emitter.onError(new AllAdaptersFailedException("All adapters failed"));
                        speechOutput.doTextToSpeech("Connection failed.");
                    }

                    @Override
                    public void onStatusUpdate(String message) {

                    }

                    @Override
                    public void requestConnectionRetry(IOException e) {
                        if (emitter.isDisposed()) {
                            LOG.info("emitter.has been disposed");
                            return;
                        }
                        if (reconnectCount++ >= MAX_RECONNECT_COUNT) {
                            LOG.warn("Max count of reconnecctes reaced", e);
                        } else {
                            LOG.info("Restarting Device Connection...");
                            speechOutput.doTextToSpeech("Connection lost. Trying to reconnect.");
                        }
                    }
                }, eventBus);

                disposables.add(new Disposable() {
                    private boolean isDisposed = false;

                    @Override
                    public void dispose() {
                        LOG.info("Disposing in connectionv verification.");
                        controller.shutdown();
                        try {
                            if (socket.getInputStream() != null)
                                socket.getInputStream().close();
                            if (socket.getOutputStream() != null)
                                socket.getOutputStream().close();
                            socket.close();
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                        isDisposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return isDisposed;
                    }
                });
//                emitter.setCancellable(() -> controller.shutdown());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            }
        }));
    }

    private ObservableTransformer<BluetoothSocketWrapper, Measurement> receiveMeasurements() {
        return upstream -> {
            final int samplingRate = ApplicationSettings.getSamplingRate(context) * 1000;
            try {
                eventBus.register(measurementProvider);
            } catch (Exception e) {
            }
            return upstream.flatMap(socket -> measurementProvider.measurements(samplingRate));
        };
    }

    private ObservableTransformer<Measurement, Measurement> enhanceMeasurements() {
        return upstream -> upstream.map(measurement -> {
            LOG.info("Receieved next recorded measurement.");
            try {
                if (!measurement.hasProperty(Measurement.PropertyKey.MAF)) {
                    try {
                        measurement.setProperty(Measurement.PropertyKey.CALCULATED_MAF, mafAlgorithm.calculateMAF(measurement));
                    } catch (NoMeasurementsException e) {
                        LOG.warn(e.getMessage());
                    }
                }

                if (consumptionAlgorithm != null) {
                    double consumption = consumptionAlgorithm.calculateConsumption(measurement);
                    measurement.setProperty(Measurement.PropertyKey.CONSUMPTION, consumption);
                    double co2 = consumptionAlgorithm.calculateCO2FromConsumption(consumption);
                    measurement.setProperty(Measurement.PropertyKey.CO2, co2);
                }

                try {
                    double consumption = energyConsumptionAlgorithm.calculateConsumption(measurement);
                    measurement.setProperty(Measurement.PropertyKey.ENERGY_CONSUMPTION, consumption);
                    double co2 = energyConsumptionAlgorithm.calculateCO2FromConsumption(consumption);
                    measurement.setProperty(Measurement.PropertyKey.ENERGY_CONSUMPTION_CO2, co2);
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            } catch (FuelConsumptionException e) {
                LOG.warn(e.getMessage());
            } catch (UnsupportedFuelTypeException e) {
                LOG.warn(e.getMessage());
            }
            return measurement;
        });
    }

    private void stopOBDConnectionRecognizer() {
        try {
            eventBus.unregister(recognizer);
            recognizer.shutDown();
            recognizer = null;
        } catch (Exception ex) {
        }
    }

    private CycleCommandProfile getCycleCommandProfile(String campaign) {
        if (campaign.equals(this.context.getString(R.string.item_campaign_profile_dvfo))) {
            return new CampagneCommandProfile();
        } else {
            return new CycleCommandProfile.Default();
        }
    }

    private final class OBDConnectionRecognizer {
        private static final long OBD_INTERVAL = 1000 * 10; // 10 seconds;
        private static final long GPS_PENDING_INTERVAL = 1000 * 30; // 30 seconds

        private long timeLastSpeedMeasurement;
        private long timeLastGpsMeasurement;
        private boolean isRunning = true;

        private final Scheduler.Worker mBackgroundWorker = Schedulers.newThread().createWorker();
        private Disposable mOBDCheckerSubscription;
        private Disposable mGPSCheckerSubscription;
        private Disposable gpsPendingSubscription;

        private final Runnable gpsConnectionCloser = () -> {
            if (!isRunning) {
                return;
            }

            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopRecording();

        };

        private final Runnable gpsNotChangedNotifier = () -> {
            if (!isRunning) {
                return;
            }
            LOG.warn("No GPS values. Connection may be closed.");
            RecordingErrorEvent event = new RecordingErrorEvent(RecordingError.NO_GPS,
                    String.format("GPS connection error. No new GPS values since %s seconds.", gpsConnectionDuration));
            event.addExtraData(RECORDING_ERROR_EXTRA_DATA_KEY, String.valueOf(gpsConnectionDuration));
            eventBus.post(event);
            // Just wait for another 30 seconds, whether user decides for continuing recording or not.
            // If there is no decision during this time interval, stop recording
            gpsPendingSubscription = mBackgroundWorker.schedule(gpsConnectionCloser,
                    GPS_PENDING_INTERVAL, TimeUnit.MILLISECONDS);
        };

        private final Runnable obdConnectionCloser = () -> {
            if (!isRunning) {
                return;
            }

            LOG.warn("CONNECTION CLOSED due to no OBD values");
            stopRecording();
        };

        @Subscribe
        public void onReceiveContinueRecordingEvent(TrackRecordingContinueEvent event) {
            LOG.info("Continue recording of track '%s' despite missing GPS connection." +
                            " No stop required via GPS Connection Recognizer.",
                    String.valueOf(event.getTrack().getName()));
            scheduleGpsConnection();
        }

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            LOG.info("Received GPS Update. No stop required via GPS Connection Recognizer.");
            scheduleGpsConnection();
        }

        @Subscribe
        public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
            if (isRunning) {
                LOG.info("Received speed update. No stop required via OBD Connection Recognizer.");
                if (mOBDCheckerSubscription != null) {
                    mOBDCheckerSubscription.dispose();
                    mOBDCheckerSubscription = null;
                }

                timeLastSpeedMeasurement = System.currentTimeMillis();

                mOBDCheckerSubscription = mBackgroundWorker.schedule(
                        obdConnectionCloser, OBD_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }

        private void scheduleGpsConnection() {
            if (isRunning) {
                LOG.info("Received GPS Update. No stop required via GPS Connection Recognizer");
                if (gpsPendingSubscription != null) {
                    gpsPendingSubscription.dispose();
                    gpsPendingSubscription = null;
                }
                if (mGPSCheckerSubscription != null) {
                    mGPSCheckerSubscription.dispose();
                    mGPSCheckerSubscription = null;
                }

                timeLastGpsMeasurement = System.currentTimeMillis();

                mGPSCheckerSubscription = mBackgroundWorker.schedule(
                        gpsNotChangedNotifier, gpsConnectionDuration * 1000, TimeUnit.MILLISECONDS);
            }
        }

        public void shutDown() {
            LOG.info("shutDown() OBDConnectionRecognizer");
            this.isRunning = false;
            if (mOBDCheckerSubscription != null) {
                mOBDCheckerSubscription.dispose();
            }
            if (mGPSCheckerSubscription != null) {
                mGPSCheckerSubscription.dispose();
            }
        }
    }
}
