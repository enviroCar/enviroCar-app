package org.envirocar.app.recording.strategy;

import android.app.Service;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.recording.TrackDatabaseSink;
import org.envirocar.app.services.OBDConnectionHandler;
import org.envirocar.app.services.recording.SpeechOutput;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.consumption.ConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.consumption.LoadBasedEnergyConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.statistics.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.obd.ConnectionListener;
import org.envirocar.obd.OBDController;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.obd.exception.AllAdaptersFailedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
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

    @Inject
    @InjectApplicationScope
    protected Context context;
    @Inject
    protected Bus eventBus;
    @Inject
    protected SpeechOutput speechOutput;
    @Inject
    protected BluetoothHandler bluetoothHandler;
    @Inject
    protected OBDConnectionHandler obdConnectionHandler;
    @Inject
    protected MeasurementProvider measurementProvider;
    @Inject
    protected TrackDatabaseSink trackDatabaseSink;

    //
    private CompositeDisposable disposables = new CompositeDisposable();
    private RecordingListener listener;
    private OBDConnectionRecognizer recognizer = new OBDConnectionRecognizer();

    // computation algorithms
    private final Car car;
    private ConsumptionAlgorithm consumptionAlgorithm;
    private CalculatedMAFWithStaticVolumetricEfficiency mafAlgorithm;
    private LoadBasedEnergyConsumptionAlgorithm energyConsumptionAlgorithm;

    /**
     * Constructor.
     */
    public OBDRecordingStrategy(Car car) {
        this.car = car;

        // set the car specific properties.
        this.consumptionAlgorithm = ConsumptionAlgorithm.fromFuelType(car.getFuelType());
        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(car);
        this.energyConsumptionAlgorithm = new LoadBasedEnergyConsumptionAlgorithm(car.getFuelType());
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        LOG.info("Destroying OBDRecordingStrategy");

        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
            disposables = null;
        }
    }

    @Override
    public void startRecording(Service service, RecordingListener listener) {
        this.listener = listener;

        // Establishing bluetooth connection -> returns BluetoothSocketWrapper with connection.
        disposables.add(
                obdConnectionHandler.getOBDConnectionObservable(bluetoothHandler.getSelectedBluetoothDevice())
                        .compose(verifyConnection())
                        .compose(receiveMeasurements())
                        .compose(enhanceMeasurements())
                        .compose(trackDatabaseSink.storeInDatabase())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnDispose(() -> listener.onRecordingStateChanged(RecordingState.RECORDING_STOPPED))
                        .subscribeWith(initializeObserver()));
    }

    @Override
    public void stopRecording() {
        LOG.info("Stopping the track recording.");
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
            disposables = null;
        }

        stopOBDConnectionRecognizer();
    }

    private DisposableObserver<Track> initializeObserver() {
        return new DisposableObserver<Track>() {
            private Track track;

            @Override
            protected void onStart() {
                LOG.info("Starting the Bluetooth connection to the selected adapter");
                listener.onRecordingStateChanged(RecordingState.RECORDING_INIT);

                try {
                    recognizer = new OBDConnectionRecognizer();
                    eventBus.register(recognizer);
                } catch (Exception e){
                    LOG.error(e.getMessage(), e);
                }
            }

            @Override
            public void onNext(Track o) {
                LOG.info(String.format("Started new Track with ID=%s", o.getTrackID()));
                this.track = o;
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
                stopOBDConnectionRecognizer();
            }
        };
    }

    private ObservableTransformer<BluetoothSocketWrapper, BluetoothSocketWrapper> verifyConnection() {
        return upstream -> upstream.flatMap(socket -> Observable.create(emitter -> {
            LOG.info(String.format("OBDConnectionService.onDeviceConntected(%s)", socket.getRemoteDeviceName()));
            speechOutput.doTextToSpeech("Connection established.");
            try {
                OBDController controller = new OBDController(socket, new ConnectionListener() {
                    int reconnectCount = 0;

                    @Override
                    public void onConnectionVerified() {
                        LOG.info("Connection verified. Starting to read measurements.");
                        listener.onRecordingStateChanged(RecordingState.RECORDING_RUNNING);
                        emitter.onNext(socket);
                    }

                    @Override
                    public void onAllAdaptersFailed() {
                        LOG.info("All adapters failed. Failed to connect to OBD adaper.");
                        emitter.onError(new AllAdaptersFailedException("All adapters failed"));
                    }

                    @Override
                    public void onStatusUpdate(String message) {

                    }

                    @Override
                    public void requestConnectionRetry(IOException e) {
                        if (reconnectCount++ >= MAX_RECONNECT_COUNT) {
                            LOG.warn("Max count of reconnecctes reaced", e);
                        } else {
                            LOG.info("Restarting Device Connection...");
                            speechOutput.doTextToSpeech("Connection lost. Trying to reconnect.");
                        }
                    }
                }, eventBus);

                emitter.setCancellable(() -> controller.shutdown());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            }
        }));
    }

    private ObservableTransformer<BluetoothSocketWrapper, Measurement> receiveMeasurements() {
        return upstream -> {
            final Long samplingRate = PreferencesHandler.getSamplingRate(context) * 1000;
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

    private void stopOBDConnectionRecognizer(){
        try {
            eventBus.unregister(recognizer);
            recognizer = null;
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
        }
    }

    private final class OBDConnectionRecognizer {
        private static final long OBD_INTERVAL = 1000 * 10; // 10 seconds;
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;

        private long timeLastSpeedMeasurement;
        private long timeLastGpsMeasurement;

        private final Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();
        private Disposable mOBDCheckerSubscription;
        private Disposable mGPSCheckerSubscription;

        private final Runnable gpsConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopRecording();
        };

        private final Runnable obdConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no OBD values");
            stopRecording();
        };

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (mGPSCheckerSubscription != null) {
                mGPSCheckerSubscription.dispose();
                mGPSCheckerSubscription = null;
            }

            timeLastGpsMeasurement = System.currentTimeMillis();

            mGPSCheckerSubscription = mBackgroundWorker.schedule(
                    gpsConnectionCloser, GPS_INTERVAL, TimeUnit.MILLISECONDS);
        }

        @Subscribe
        public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
            LOG.info("Received speed update, no stop required via mOBDCheckerSubscription!");
            if (mOBDCheckerSubscription != null) {
                mOBDCheckerSubscription.dispose();
                mOBDCheckerSubscription = null;
            }

            timeLastSpeedMeasurement = System.currentTimeMillis();

            mOBDCheckerSubscription = mBackgroundWorker.schedule(
                    obdConnectionCloser, OBD_INTERVAL, TimeUnit.MILLISECONDS);
        }

        public void shutDown() {
            LOG.info("shutDown() OBDConnectionRecognizer");
            if (mOBDCheckerSubscription != null)
                mOBDCheckerSubscription.dispose();
            if (mGPSCheckerSubscription != null)
                mGPSCheckerSubscription.dispose();
        }
    }
}
