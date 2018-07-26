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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.widget.RemoteViews;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.squareup.otto.Subscribe;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.notifications.NotificationActionHolder;
import org.envirocar.app.notifications.ServiceStateForNotificationForNotification;
import org.envirocar.app.views.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.NewMeasurementEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.storage.EnviroCarDB;

import java.text.DecimalFormat;
import java.util.ArrayList;
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

/***
 @author Sai Krishna

 Service that handles GPS based track recording
***/
public class GPSOnlyConnectionService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(GPSOnlyConnectionService.class);

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    public static void startService(Context context){
        ServiceUtils.startService(context, GPSOnlyConnectionService.class);
    }

    public static void stopService(Context context){
        ServiceUtils.stopService(context, GPSOnlyConnectionService.class);
    }


    public static BluetoothServiceState CURRENT_SERVICE_STATE = BluetoothServiceState
            .SERVICE_STOPPED;
    public static boolean drivingDetected = false;

    //Activity recognition stuff
    // The intent action which will be fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private PendingIntent mPendingIntent;

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

    // Text to speech variables.
    private TextToSpeech mTTS;
    private boolean mIsTTSAvailable;
    private boolean mIsTTSPrefChecked;

    // Different subscriptions
    private Subscription mTTSPrefSubscription;
    private Subscription mMeasurementSubscription;
    private Subscription mDrivingStoppedSubscription;

    private final Action0 drivingConnectionCloser = () -> {
        LOG.warn("CONNECTION CLOSED due to driving state absence");
        stopGPSOnlyConnection();
        stopSelf();
    };
    //2 times the average latency of the activity transition library i.e. 2*55 sec
    private static final long DRIVING_INTERVAL = 1000 * 55 * 2;

    // This satellite fix indicates that there is no satellite connection yet.
    private GpsSatelliteFix mCurrentGpsSatelliteFix = new GpsSatelliteFix(0, false);
    private GPSOnlyConnectionRecognizer connectionRecognizer = new GPSOnlyConnectionRecognizer();

    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();
    NotificationManager notificationManager;
    private String CHANNEL_ID = "channel1";

    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";

    private int mAvrgSpeed = 0;
    private double mDistanceValue = 0.0;
    private long mStartingTime = 0;
    private boolean isTrackStarted = false;
    Notification notification;
    // Broadcast receiver that handles the stopping of the track that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Received action matches the command for stopping the recording process of a track.
            if (ACTION_STOP_TRACK_RECORDING.equals(action)) {
                LOG.info("Received Broadcast: Stop Track Recording.");

                // Finish the current track.
                trackRecordingHandler.finishCurrentTrack();
            }else if(TRANSITIONS_RECEIVER_ACTION.equals(action)){
                if (ActivityTransitionResult.hasResult(intent)) {
                    ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                        if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                            drivingDetected = true;
                            bus.post(new DrivingDetectedEvent(drivingDetected));
                            if (mDrivingStoppedSubscription != null) {
                                mDrivingStoppedSubscription.unsubscribe();
                                mDrivingStoppedSubscription = null;
                            }
                        }else if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                            if(CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STARTED){
                                mDrivingStoppedSubscription = backgroundWorker.schedule(
                                        drivingConnectionCloser, DRIVING_INTERVAL, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public void onCreate() {
        LOG.info("GPSOnlyConnectionService.onCreate()");
        super.onCreate();

        notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        // register on the event bus
        this.bus.register(this);
        this.bus.register(mTrackDetailsProvider);
        this.bus.register(connectionRecognizer);
        this.bus.register(measurementProvider);

        //Activity recognition stuff
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        registerReceiver(mBroadcastReciever, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));
        setupActivityTransitions();

        // Register a new BroadcastReceiver that waits for incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_STOP_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, notificationClickedFilter);

        mTTS = new TextToSpeech(getApplicationContext(), status -> {
            try {
                if (status == TextToSpeech.SUCCESS) {
                    mTTS.setLanguage(Locale.ENGLISH);
                    mIsTTSAvailable = true;
                } else {
                    LOG.warn("TextToSpeech is not available.");
                }
            } catch(IllegalArgumentException e){
                LOG.warn("TextToSpeech is not available");
            }
        });

        mTTSPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(getApplicationContext())
                        .subscribe(aBoolean -> mIsTTSPrefChecked = aBoolean);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("GPSOnlyConnectionService.onStartCommand()");
        doTextToSpeech("Establishing connection");

        // Acquire the wake lock for keeping the CPU active.
        mWakeLock.acquire();
        // Start the location
        mLocationHandler.startLocating();

        Intent intent1 = new Intent(this, BaseMainActivityBottomBar.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent1, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this,CHANNEL_ID)
                    .setContentTitle(getBaseContext().getString(ServiceStateForNotificationForNotification.CONNECTING.getTitle()))
                    .setContentText(getBaseContext().getString(ServiceStateForNotificationForNotification.CONNECTING.getSubText()))
                    .setSmallIcon(ServiceStateForNotificationForNotification.CONNECTING.getIcon())
                    .setContentIntent(pIntent)
                    .setAutoCancel(true).build();

            startForeground(181,notification);
        }else{

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(getBaseContext().getString(ServiceStateForNotificationForNotification.CONNECTING.getTitle()))
                    .setContentText(getBaseContext().getString(ServiceStateForNotificationForNotification.CONNECTING.getSubText()))
                    .setSmallIcon(ServiceStateForNotificationForNotification.CONNECTING.getIcon())
                    .setContentIntent(pIntent)
                    .setAutoCancel(true).build();

            startForeground(181,notification);
        }

        LOG.info("Starting the GPS Only connection");
        // Start the OBD Connection.
        startGPSOnlyConnection();

        return START_STICKY;
    }

    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        mAvrgSpeed = event.mAvrgSpeed;
        refreshNotification();
    }

    @Subscribe
    public void onReceiveDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        mDistanceValue = event.mDistanceValue;
        refreshNotification();
    }

    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        mStartingTime = event.mStartingTime;
        isTrackStarted = event.mIsStarted;
        refreshNotification();
    }

    /**
     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupActivityTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        LOG.info("Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        LOG.warn("Transitions Api could not be registered: " + e,null);
                    }
                });
    }

    private void refreshNotification(){

        Intent intent = new Intent(getBaseContext(), GPSOnlyTrackRecordingScreen.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intent, 0);

        RemoteViews notificationBigLayout = new RemoteViews(getPackageName(), R.layout.notification_while_track_recording);
        RemoteViews notificationSmallLayout = new RemoteViews(getPackageName(), R.layout.notification_while_track_recording_small);

        NotificationActionHolder actionHolder = ServiceStateForNotificationForNotification.CONNECTED.getAction(getBaseContext());
        notificationBigLayout.setOnClickPendingIntent(R.id.notification_obd_service_state_button, actionHolder.actionIntent);
        notificationBigLayout.setTextViewText(R.id.notification_distance, String.format("%s km", DECIMAL_FORMATTER.format(mDistanceValue)));
        notificationBigLayout.setTextViewText(R.id.notification_speed, String.format("%s km/h", Integer.toString(mAvrgSpeed)));
        notificationBigLayout.setChronometer(R.id.notification_timertext,mStartingTime,"%s",isTrackStarted);
        notificationSmallLayout.setTextViewText(R.id.notification_distance, String.format("%s km", DECIMAL_FORMATTER.format(mDistanceValue)));
        notificationSmallLayout.setTextViewText(R.id.notification_speed, String.format("%s km/h", Integer.toString(mAvrgSpeed)));
        notificationSmallLayout.setChronometer(R.id.notification_timertext,mStartingTime,"%s",isTrackStarted);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            notification = new Notification.Builder(getBaseContext(),CHANNEL_ID)
                    .setSmallIcon(ServiceStateForNotificationForNotification.CONNECTED.getIcon())
                    .setContentIntent(pIntent)
                    .setCustomContentView(notificationSmallLayout)
                    .setCustomBigContentView(notificationBigLayout)
                    .setAutoCancel(true).build();

        }else{
            notification = new Notification.Builder(getBaseContext())
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(ServiceStateForNotificationForNotification.CONNECTED.getIcon())
                    .setContentIntent(pIntent)
                    .setContent(notificationSmallLayout)
                    .setAutoCancel(true).build();

            notification.bigContentView = notificationBigLayout;
        }
        notificationManager.notify(181,notification);

    }

    /**
     * Sets the current remoteService state and fire an event on the bus.
     *
     * @param state the state of the remoteService.
     */
    private void setTrackRecordingServiceState(BluetoothServiceState state) {
        // Set the new remoteService state
        CURRENT_SERVICE_STATE = state; // TODO FIX
        // and fire an event on the event bus.
        this.bus.post(produceTrackRecordingServiceStateChangedEvent());
    }

    public TrackRecordingServiceStateChangedEvent produceTrackRecordingServiceStateChangedEvent() {
        LOG.info(String.format("produceBluetoothServiceStateChangedEvent(): %s",
                CURRENT_SERVICE_STATE.toString()));
        return new TrackRecordingServiceStateChangedEvent(CURRENT_SERVICE_STATE);
    }

    @Override
    public void onDestroy() {
        LOG.info("GPSOnlyConnectionService.onDestroy()");
        super.onDestroy();

        // Stop this remoteService and emove this remoteService from foreground state.
        stopGPSOnlyConnection();

        // Unregister from the event bus.
        bus.unregister(this);
        bus.unregister(mTrackDetailsProvider);
        bus.unregister(connectionRecognizer);
        bus.unregister(measurementProvider);

        // unregister all boradcast receivers.
        unregisterReceiver(mBroadcastReciever);

        // Unregister the transitions:
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(aVoid -> LOG.info("Transitions successfully unregistered."))
                .addOnFailureListener(e -> LOG.warn("Transitions could not be unregistered: " + e));

        drivingDetected = false;
        LOG.info("GPSOnlyConnectionService successfully destroyed");
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
     * Method that starts the remoteService
     */
    private void startGPSOnlyConnection(){
        // Set remoteService state to STARTING and fire an event on the bus.

        setTrackRecordingServiceState(BluetoothServiceState.SERVICE_STARTED);
        subscribeForMeasurements();
        mStartingTime = SystemClock.elapsedRealtime();
        LOG.info("Setting the custom notification while recording for the first time.");
        refreshNotification();

    }


    /**
     * Method that stops the remoteService, removes everything from the waiting list
     */
    private void stopGPSOnlyConnection() {
        LOG.info("stopGPSOnlyConnection called");
        backgroundWorker.schedule(() -> {
            stopForeground(true);

            if (mTTSPrefSubscription != null && !mTTSPrefSubscription.isUnsubscribed())
                mTTSPrefSubscription.unsubscribe();
            if (mMeasurementSubscription != null && !mMeasurementSubscription.isUnsubscribed())
                mMeasurementSubscription.unsubscribe();

            if (connectionRecognizer != null)
                connectionRecognizer.shutDown();
            if (mTrackDetailsProvider != null)
                mTrackDetailsProvider.clear();
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            if (mDrivingStoppedSubscription != null) {
                mDrivingStoppedSubscription.unsubscribe();
                mDrivingStoppedSubscription = null;
            }

            mLocationHandler.stopLocating();
            doTextToSpeech("Device disconnected");

            // Set state of the remoteService to stopped.
            setTrackRecordingServiceState(BluetoothServiceState.SERVICE_STOPPED);
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
                measurementPublisher.onNext(measurement);
                bus.post(new NewMeasurementEvent(measurement));
            }
        };
    }



    private final class GPSOnlyConnectionRecognizer {
        private static final long GPS_INTERVAL = 1000 * 60 * 2; // 2 minutes;

        private final Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();
        private Subscription mGPSCheckerSubscription;

        private final Action0 gpsConnectionCloser = () -> {
            LOG.warn("CONNECTION CLOSED due to no GPS values");
            stopGPSOnlyConnection();
            stopSelf();
        };

        @Subscribe
        public void onReceiveGpsLocationChangedEvent(GpsLocationChangedEvent event) {
            if (mGPSCheckerSubscription != null) {
                mGPSCheckerSubscription.unsubscribe();
                mGPSCheckerSubscription = null;
            }
            mGPSCheckerSubscription = mBackgroundWorker.schedule(
                    gpsConnectionCloser, GPS_INTERVAL, TimeUnit.MILLISECONDS);
        }

        public void shutDown() {
            LOG.info("shutDown() GPSOnlyConnectionRecognizer");
            if (mGPSCheckerSubscription != null)
                mGPSCheckerSubscription.unsubscribe();
        }
    }

}
