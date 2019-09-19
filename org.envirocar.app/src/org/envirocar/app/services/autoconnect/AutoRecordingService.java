/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.services.autoconnect;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.ScopedBaseInjectorService;
import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.recording.events.RecordingTypeSelectedEvent;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

import static org.envirocar.app.notifications.NotificationHandler.context;

/**
 * @author dewall
 */
public class AutoRecordingService extends ScopedBaseInjectorService implements AutoRecordingStrategy.AutoRecordingCallback {
    private static final Logger LOG = Logger.getLogger(AutoRecordingService.class);

    public static final void startService(Context context) {
        ServiceUtils.startService(context, AutoRecordingService.class);
    }

    public static final void stopService(Context context) {
        ServiceUtils.stopService(context, AutoRecordingService.class);
    }

    private static final int REDISCOVERY_INTERVAL = 30;

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";

    // Injected variables
    @Inject
    protected AutoRecordingStrategy.Factory factory;


    private boolean isAutoConnectEnabled = PreferencesHandler.DEFAULT_BLUETOOTH_AUTOCONNECT;
    private int mDiscoveryInterval = PreferencesHandler.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL;
    private RecordingType recordingType = PreferencesHandler.DEFAULT_RECORDING_TYPE;

    private CompositeDisposable disposables = new CompositeDisposable();
    private AutoRecordingStrategy autoStrategy = null;


    @Override
    protected void setupServiceComponent() {
        BaseApplication.get(this)
                .getBaseApplicationComponent()
                .plus(new AutoRecordingModule(this))
                .inject(this);
    }


    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }


    @Override
    public void onCreate() {
        LOG.info("onCreate()");
        super.onCreate();

        // Register on the event bus.
        this.bus.register(this);

        // Get the required preference settings.
        this.mDiscoveryInterval = PreferencesHandler.getDiscoveryInterval(context);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_START_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_STOP_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_START_TRACK_RECORDING);
        this.disposables.add(
                RxBroadcastReceiver.create(this, notificationClickedFilter)
                        .doOnNext(this::onReceiveNotificationIntent)
                        .doOnError(LOG::error)
                        .subscribe());


        initPreferenceSubscriptions();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        LOG.info("onStartCommand()");
        this.updateAutoRecording();
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");
        super.onDestroy();

        this.bus.unregister(this);

        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
    }




    @Override
    public void onPreconditionUpdate(AutoRecordingStrategy.PreconditionType preconditionType) {
        switch (preconditionType) {
            case BT_DISABLED:
                break;
            case GPS_DISABLED:
                break;
            case OBD_NOT_SELECTED:
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_OBD_SELECTED);
                break;
            case CAR_NOT_SELECTED:
                NotificationHandler.setRecordingState(ServiceStateForNotification.NO_CAR_SELECTED);
                break;
        }
    }

    @Override
    public void onRecordingTypeConditionsMet() {
        LOG.info("Conditions to start a track are satisfied. Starting the recording service");
        this.autoStrategy.stop();
        this.startRecordingService();
    }

    @Subscribe
    public void onRecordingTypeSelectedEvent(RecordingTypeSelectedEvent event) {
        LOG.info("Received event [%]", event.toString());
        if (this.recordingType != event.recordingType &&
                RecordingService.RECORDING_STATE != RecordingState.RECORDING_RUNNING) {
            this.recordingType = event.recordingType;
            updateAutoRecording();
        } else {
            this.recordingType = event.recordingType;
        }
    }

    @Subscribe
    public void onRecordingStateEvent(RecordingStateEvent event) {
        LOG.info("Received event [%s]", event.toString());
        switch (event.recordingState) {
            case RECORDING_INIT:
                this.autoStrategy.stop();
                break;
            case RECORDING_RUNNING:
                stopSelf();
                break;
            case RECORDING_STOPPED:

                break;
        }
    }

    /**
     * Broadcast receiver that handles the different actions that could be issued by the
     * corresponding notification of the notification bar.
     *
     * @param intent
     */
    private void onReceiveNotificationIntent(Intent intent) {
        String action = intent.getAction();

        // Received action matches the command for starting the discovery process for the
        // selected OBDII-Adapter.
        if (ACTION_START_BT_DISCOVERY.equals(action)) {
            LOG.info("Received Broadcast: Start Discovery.");

        }

        // Received action matches the command for stopping the Bluetooth discovery process.
        else if (ACTION_STOP_BT_DISCOVERY.equals(action)) {
            LOG.info("Received Broadcast: Stop Discovery.");

            // Set the notification state to unconnected.
            NotificationHandler.setRecordingState(ServiceStateForNotification.UNCONNECTED);

        }

        // Received action matches the command for starting the recording of a track.
        else if (ACTION_START_TRACK_RECORDING.equals(action)) {
            LOG.info("Received Broadcast: Start Track Recording.");

        }
    }

    private void initPreferenceSubscriptions() {
//        disposables.add(
//                PreferencesHandler.getSelectedRecordingTypeObservable(getApplicationContext())
//                        .doOnNext(recordingType ->)
//                        .doOnError(LOG::error)
//                        .subscribe());

        disposables.add(
                PreferencesHandler.getAutoconnectObservable(this)
                        .doOnNext(isAutoConnectEnabled -> {
                            this.isAutoConnectEnabled = isAutoConnectEnabled;
                            updateAutoRecording();
                        })
                        .doOnError(LOG::error)
                        .subscribe());
    }

    private void updateAutoRecording() {
        if (this.autoStrategy != null) {
            this.autoStrategy.stop();
            getLifecycle().removeObserver(this.autoStrategy);
        }

        this.autoStrategy = this.factory.create();
        getLifecycle().addObserver(this.autoStrategy);
        this.autoStrategy.run(this);
    }

    /**
     * Starts the GPSOnlyConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startRecordingService() {
        if (!ServiceUtils.isServiceRunning(getApplicationContext(), RecordingService.class)) {
            // Start the GPS Only Connection Service
            getApplicationContext().startService(new Intent(this, RecordingService.class));
        }
    }


}
