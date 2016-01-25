/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;

import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.NewMeasurementEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.injection.BaseInjectorService;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.AbstractCalculatedMAFAlgorithm;
import org.envirocar.core.trackprocessing.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.core.trackprocessing.ConsumptionAlgorithm;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.obd.ConnectionListener;
import org.envirocar.obd.OBDController;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.storage.EnviroCarDB;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * @author dewall
 */
public class OBDConnectionService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(OBDConnectionService.class);

    protected static final int MAX_RECONNECT_COUNT = 2;
    public static final int BG_NOTIFICATION_ID = 42;

    public static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState
            .SERVICE_STOPPED;

    // Injected fields.
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected LocationHandler mLocationHandler;
    @Inject
    protected TrackDetailsProvider mTrackDetailsProvider;
    @Inject
    protected PowerManager.WakeLock mWakeLock;
    @Inject
    protected MeasurementProvider measurementProvider;
    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;
    @Inject
    protected TrackRecordingHandler trackRecordingHandler;
    @Inject
    protected OBDConnectionHandler obdConnectionHandler;

    private AbstractCalculatedMAFAlgorithm mafAlgorithm;

    // Text to speech variables.
    private TextToSpeech mTTS;
    private boolean mIsTTSAvailable;
    private boolean mIsTTSPrefChecked;

    // Member fields required for the connection to the OBD device.
    private OBDController mOBDController;

    // Different subscriptions
    private Subscription mTTSPrefSubscription;
    private Subscription mConnectingSubscription;
    private Subscription mMeasurementSubscription;

    private BluetoothSocketWrapper bluetoothSocketWrapper;

    // This satellite fix indicates that there is no satellite connection yet.
    private GpsSatelliteFix mCurrentGpsSatelliteFix = new GpsSatelliteFix(0, false);

    private OBDConnectionRecognizer connectionRecognizer = new OBDConnectionRecognizer();
    private ConsumptionAlgorithm consumptionAlgorithm;

    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    @Override
    public void onCreate() {
        LOG.info("OBDConnectionService.onCreate()");
        super.onCreate();

        // register on the event bus
        this.bus.register(this);
        this.bus.register(mTrackDetailsProvider);
        this.bus.register(connectionRecognizer);
        this.bus.register(measurementProvider);

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    mIsTTSAvailable = true;
                    mTTS.setLanguage(Locale.ENGLISH);
                } else {
                    LOG.warn("TextToSpeech is not available.");
                }
            }
        });

        mTTSPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            mIsTTSPrefChecked = aBoolean;
                        });

        /**
         * create the consumption and MAF algorithm, final for this connection
         */
        Car car = carHandler.getCar();
        this.consumptionAlgorithm = CarUtils.resolveConsumptionAlgorithm(car.getFuelType());

        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(car);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("OBDConnectionService.onStartCommand()");
        doTextToSpeech("Establishing connection");

        // Acquire the wake lock for keeping the CPU active.
        mWakeLock.acquire();
        // Start the location
        mLocationHandler.startLocating();

        // Get the default device
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device != null) {
            LOG.info("The BluetoothHandler has a valid device. Start the OBD connection");

            // Start the OBD Connection.
            mConnectingSubscription = startOBDConnection(device);
        } else {
            LOG.severe("No default Bluetooth device selected");
        }

        return START_STICKY;
    }

    /**
     * Sets the current remoteService state and fire an event on the bus.
     *
     * @param state the state of the remoteService.
     */
    private void setBluetoothServiceState(BluetoothServiceState state) {
        // Set the new remoteService state
        CURRENT_SERVICE_STATE = state; // TODO FIX
        // and fire an event on the event bus.
        this.bus.post(produceBluetoothServiceStateChangedEvent());
    }

    //    @Produce
    public BluetoothServiceStateChangedEvent produceBluetoothServiceStateChangedEvent() {
        LOG.info(String.format("produceBluetoothServiceStateChangedEvent(): %s",
                CURRENT_SERVICE_STATE.toString()));
        return new BluetoothServiceStateChangedEvent(CURRENT_SERVICE_STATE);
    }


    @Override
    public void onDestroy() {
        LOG.info("OBDConnectionService.onDestroy()");
        super.onDestroy();

        // Stop this remoteService and emove this remoteService from foreground state.
        stopOBDConnection();

        // Unregister from the event bus.
        bus.unregister(this);
        bus.unregister(mTrackDetailsProvider);
        bus.unregister(connectionRecognizer);
        bus.unregister(measurementProvider);

        LOG.info("OBDConnectionService successfully destroyed");
    }

    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new OBDServiceModule());
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        boolean isFix = event.mGpsSatelliteFix.isFix();
        if (isFix != mCurrentGpsSatelliteFix.isFix()) {
            if (isFix) {
                doTextToSpeech("GPS positioning established");
            } else {
                doTextToSpeech("GPS positioning lost. Try to move the phone");
            }
            this.mCurrentGpsSatelliteFix = event.mGpsSatelliteFix;
        }
    }

    private void doTextToSpeech(String string) {
        if (mIsTTSAvailable && mIsTTSPrefChecked) {
            mTTS.speak(string, TextToSpeech.QUEUE_ADD, null);
        }
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
                        setBluetoothServiceState(BluetoothServiceState.SERVICE_STARTING);
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

    private void onDeviceConnected(BluetoothSocketWrapper bluetoothSocket) {
        LOG.info(String.format("OBDConnectionService.onDeviceConntected(%s)",
                bluetoothSocket.getRemoteDeviceName()));
        try {
            this.mOBDController = new OBDController(bluetoothSocket, new ConnectionListener() {
                private int mReconnectCount = 0;

                @Override
                public void onConnectionVerified() {
                    setBluetoothServiceState(BluetoothServiceState.SERVICE_STARTED);
                    subscribeForMeasurements();
                }

                @Override
                public void onAllAdaptersFailed() {
                    LOG.info("all adapters failed!");
                    stopOBDConnection();
                    doTextToSpeech("failed to connect to the OBD adapter");
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
                        doTextToSpeech("Connection lost. Trying to reconnect.");
                    }
                }
            }, bus);
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            stopSelf();
            return;
        }

        doTextToSpeech("Connection established");
    }


    /**
     * Method that stops the remoteService, removes everything from the waiting list
     */
    private void stopOBDConnection() {
        LOG.info("stopOBDConnection called");
        backgroundWorker.schedule(() -> {
            stopForeground(true);

            // If there is an active UUID subscription.
            if (mConnectingSubscription != null && !mConnectingSubscription.isUnsubscribed())
                mConnectingSubscription.unsubscribe();
            if (mTTSPrefSubscription != null && !mTTSPrefSubscription.isUnsubscribed())
                mTTSPrefSubscription.unsubscribe();
            if (mMeasurementSubscription != null && !mMeasurementSubscription.isUnsubscribed())
                mMeasurementSubscription.unsubscribe();

            if (mOBDController != null)
                mOBDController.shutdown();
            if (bluetoothSocketWrapper != null)
                bluetoothSocketWrapper.shutdown();
            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (mTrackDetailsProvider != null)
                mTrackDetailsProvider.clear();
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            mLocationHandler.stopLocating();
            showServiceStateStoppedNotification();
            doTextToSpeech("Device disconnected");

            // Set state of the remoteService to stopped.
            setBluetoothServiceState(BluetoothServiceState.SERVICE_STOPPED);
        });
    }


    private void subscribeForMeasurements() {
        // this is the first access to the measurement objects push it further
        Long samplingRate = PreferencesHandler.getSamplingRate(getApplicationContext()) * 1000;
        mMeasurementSubscription = measurementProvider.measurements(samplingRate)
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
                LOG.info("onNNNNENEEXT()");
                try {
                    if (!measurement.hasProperty(Measurement.PropertyKey.MAF)) {
                        try {
                            measurement.setProperty(Measurement.PropertyKey
                                    .CALCULATED_MAF, mafAlgorithm.calculateMAF(measurement));
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
                bus.post(new NewMeasurementEvent(measurement));
            }
        };
    }


    private void showServiceStateStoppedNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);
        Notification noti = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("enviroCar")
                .setContentText(getResources()
                        .getText(R.string.service_state_stopped))
                .setSmallIcon(R.drawable.dashboard)
                .setAutoCancel(true)
                .build();
        manager.notify(BG_NOTIFICATION_ID, noti);
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
            stopSelf();
        };

        private final Action0 obdConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no OBD values");
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
