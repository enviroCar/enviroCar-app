package org.envirocar.app.services.recording;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.services.OBDConnectionHandler;
import org.envirocar.app.views.recordingscreen.OBDPlusGPSTrackRecordingScreen;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.core.trackprocessing.ConsumptionAlgorithm;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.ConnectionListener;
import org.envirocar.obd.OBDController;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class OBDRecordingService extends AbstractRecordingService {
    private static final Logger LOG = Logger.getLogger(OBDRecordingService.class);

    public static void stopService(Context context) {
        ServiceUtils.stopService(context, OBDRecordingService.class);
    }

    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";
    protected static final int MAX_RECONNECT_COUNT = 2;

    public static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STOPPED;

    // background worker
    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    @Inject
    protected PowerManager.WakeLock wakeLock;
    @Inject
    protected LocationHandler locationHandler;
    @Inject
    protected BluetoothHandler bluetoothHandler;
    @Inject
    protected SpeechOutput speechOutput;
    @Inject
    protected CarPreferenceHandler carPreferenceHandler;
    @Inject
    protected TrackRecordingHandler trackRecordingHandler;
    @Inject
    protected OBDConnectionHandler obdConnectionHandler;
    @Inject
    protected TrackDetailsProvider trackDetailsProvider;
    @Inject
    protected MeasurementProvider measurementProvider;

    // Member fields required for the connection to the OBD device.
    private OBDController obdController;
    private OBDConnectionRecognizer connectionRecognizer;

    // computation algorithms
    private ConsumptionAlgorithm consumptionAlgorithm;
    private CalculatedMAFWithStaticVolumetricEfficiency mafAlgorithm;

    // subscriptions
    private Subscription connectingSubscription;
    private Subscription measurementSubscription;

    private BluetoothSocketWrapper bluetoothSocketWrapper;

    // Broadcast receiver that handles the stopping of the track that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver broadcastReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Received action matches the command for stopping the recording process of a track.
            if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                LOG.info("Received Broadcast: Stop Track Recording.");

                // Finish the current track.
                trackRecordingHandler.finishCurrentTrack();
            }
        }
    };

    @Override
    public void onCreate() {
        // Connection Recognizer
        this.connectionRecognizer = new OBDConnectionRecognizer();
        this.eventBusReceivers.add(this.connectionRecognizer);

        //
        super.onCreate();

        // Register a new BroadcastReceiver that waits for incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(broadcastReciever, notificationClickedFilter);

        // car specific algorithms and preferences
        Car car = carPreferenceHandler.getCar();
        this.consumptionAlgorithm = CarUtils.resolveConsumptionAlgorithm(car.getFuelType());
        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(car);
    }


    @Override
    protected void startRecording() {
        // Get the default device
        BluetoothDevice device = bluetoothHandler.getSelectedBluetoothDevice();
        if (device != null) {
            LOG.info("The BluetoothHandler has a valid device. Start the OBD Connection");

            //
            connectingSubscription = startOBDConnection(device);
        } else {
            LOG.severe("No default Bluetooth device selected");
            ServiceUtils.stopService(this, OBDRecordingService.class);
        }
    }

    @Override
    protected void stopRecording() {
        LOG.info("Destroying the OBD-based recording");
        super.onDestroy();

        unregisterReceiver(broadcastReciever);

        LOG.info("OBDConnectionService successfully destroyed");
    }


    @Override
    protected Class<? extends BaseInjectorActivity> getRecordingScreenClass() {
        return OBDPlusGPSTrackRecordingScreen.class;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    /**
     * @param device the device to start a connection to.
     */
    private Subscription startOBDConnection(final BluetoothDevice device) {
        return obdConnectionHandler.getOBDConnectionObservable(device)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<BluetoothSocketWrapper>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() connection");

                        // Set remoteService state to STARTING and fire an event on the bus.
                        CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STARTING;
                        bus.post(new TrackRecordingServiceStateChangedEvent(CURRENT_SERVICE_STATE));
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted(): BluetoothSocketWrapper connection completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        unsubscribe();
                    }

                    @Override
                    public void onNext(BluetoothSocketWrapper socketWrapper) {
                        LOG.info("startOBDConnection.onNext() socket successfully connected.");
                        bluetoothSocketWrapper = socketWrapper;
                        onDeviceConnected(bluetoothSocketWrapper);
                        onCompleted();
                    }
                });
    }

    private void stopOBDConnection() {
        LOG.info("stopOBDConnection called");
        backgroundWorker.schedule(() -> {
            stopForeground(true);

            // If there is an active UUID subscription.
            if (connectingSubscription != null && !connectingSubscription.isUnsubscribed())
                connectingSubscription.unsubscribe();
            if (measurementSubscription != null && !measurementSubscription.isUnsubscribed())
                measurementSubscription.unsubscribe();

            if (obdController != null)
                obdController.shutdown();
            if (bluetoothSocketWrapper != null)
                bluetoothSocketWrapper.shutdown();
            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (trackDetailsProvider != null)
                trackDetailsProvider.clear();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }

            locationHandler.stopLocating();
            this.speechOutput.doTextToSpeech("Device disconnected");

            // Set state of the remoteService to stopped.
            CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STOPPED;
            bus.post(new TrackRecordingServiceStateChangedEvent(CURRENT_SERVICE_STATE));
        });
    }

    private void onDeviceConnected(BluetoothSocketWrapper bluetoothSocket) {
        LOG.info(String.format("OBDConnectionService.onDeviceConntected(%s)",
                bluetoothSocket.getRemoteDeviceName()));
        try {
            this.obdController = new OBDController(bluetoothSocket, new ConnectionListener() {
                private int mReconnectCount = 0;

                @Override
                public void onConnectionVerified() {
                    CURRENT_SERVICE_STATE = BluetoothServiceState.SERVICE_STARTED;
                    bus.post(new TrackRecordingServiceStateChangedEvent(CURRENT_SERVICE_STATE));
                    subscribeForMeasurements();
                    //mStartingTime = SystemClock.elapsedRealtime();

                }

                @Override
                public void onAllAdaptersFailed() {
                    LOG.info("all adapters failed!");
                    stopOBDConnection();
                    speechOutput.doTextToSpeech("failed to connect to the OBD adapter");
                }

                @Override
                public void onStatusUpdate(String message) {

                }

                @Override
                public void requestConnectionRetry(IOException e) {
                    if (mReconnectCount++ >= MAX_RECONNECT_COUNT) {
                        LOG.warn("Max count of reconnecctes reaced", e);
                    } else {
                        LOG.info("Restarting Device Connection...");
                        speechOutput.doTextToSpeech("Connection lost. Trying to reconnect.");
                    }
                }
            }, bus);
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            stopSelf();
            return;
        }

        this.speechOutput.doTextToSpeech("Connection established");
    }

    private void subscribeForMeasurements() {
        // this is the first access to the measurement objects push it further
        Long samplingRate = PreferencesHandler.getSamplingRate(getApplicationContext()) * 1000;
        measurementSubscription = measurementProvider.measurements(samplingRate)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(getMeasurementSubscriber());
    }


    private Subscriber<Measurement> getMeasurementSubscriber() {
        return new Subscriber<Measurement>() {
            PublishSubject<Measurement> measurementPublisher =
                    PublishSubject.create();

            @Override
            public void onStart() {
                LOG.info("onStart(): MeasuremnetProvider Subscription");
                add(trackRecordingHandler.startNewTrack(measurementPublisher));
            }

            @Override
            public void onCompleted() {
                LOG.info("onCompleted(): MeasurementProvider");
                measurementPublisher.onCompleted();
                measurementPublisher = null;
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                measurementPublisher.onError(e);
                measurementPublisher = null;
            }

            @Override
            public void onNext(Measurement measurement) {
                LOG.info("Receieved next recorded measurement.");
                try {
                    if (!measurement.hasProperty(Measurement.PropertyKey.MAF)) {
                        try {
                            measurement.setProperty(Measurement.PropertyKey.CALCULATED_MAF,
                                    mafAlgorithm.calculateMAF(measurement));
                        } catch (NoMeasurementsException e) {
                            LOG.warn(e.getMessage());
                        }
                    }

                    if (consumptionAlgorithm != null) {
                        double consumption = consumptionAlgorithm.calculateConsumption(measurement);
                        double co2 = consumptionAlgorithm.calculateCO2FromConsumption(consumption);
                        measurement.setProperty(Measurement.PropertyKey.CONSUMPTION, consumption);
                        measurement.setProperty(Measurement.PropertyKey.CO2, co2);
                    }
                } catch (FuelConsumptionException e) {
                    LOG.warn(e.getMessage());
                } catch (UnsupportedFuelTypeException e) {
                    LOG.warn(e.getMessage());
                }

                measurementPublisher.onNext(measurement);
                bus.post(new RecordingNewMeasurementEvent(measurement));
            }
        };
    }

    /**
     * Event producer method for TrackRecording states.
     *
     * @return the current service state.
     */
    @Produce
    public TrackRecordingServiceStateChangedEvent produceRecordingStateEvent() {
        return new TrackRecordingServiceStateChangedEvent(CURRENT_SERVICE_STATE);
    }


    private final class OBDConnectionRecognizer {
        private static final long OBD_INTERVAL = 1000 * 10; // 10 seconds;
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;

        private long timeLastSpeedMeasurement;
        private long timeLastGpsMeasurement;

        private final Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();
        private Subscription mOBDCheckerSubscription;
        private Subscription mGPSCheckerSubscription;

        private final Action0 gpsConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopOBDConnection();
            stopSelf();
        };

        private final Action0 obdConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no OBD values");
            stopOBDConnection();
            stopSelf();
        };

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (mGPSCheckerSubscription != null) {
                mGPSCheckerSubscription.unsubscribe();
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
                mOBDCheckerSubscription.unsubscribe();
                mOBDCheckerSubscription = null;
            }

            timeLastSpeedMeasurement = System.currentTimeMillis();

            mOBDCheckerSubscription = mBackgroundWorker.schedule(
                    obdConnectionCloser, OBD_INTERVAL, TimeUnit.MILLISECONDS);
        }

        public void shutDown() {
            LOG.info("shutDown() OBDConnectionRecognizer");
            if (mOBDCheckerSubscription != null)
                mOBDCheckerSubscription.unsubscribe();
            if (mGPSCheckerSubscription != null)
                mGPSCheckerSubscription.unsubscribe();
        }
    }

}
