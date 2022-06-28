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
package org.envirocar.app.recording.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.app.notifications.NotificationActionHolder;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RecordingNotification implements LifecycleObserver {
    private static final Logger LOG = Logger.getLogger(RecordingNotification.class);
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    // Channel_ID required for newer version
    private static final String DEFAULT_CHANNEL_ID = "com.envirocar.app.recording.notification";
    private static final int notificationId = 181;

    // Injected variables


    // context information
    private final RecordingService context;
    private final Bus eventBus;
    private final Class screenClass;
    private final NotificationManager notificationManager;
    private final String channelId;

    // Stats of the recording
    private BluetoothServiceState bluetoothServiceState;
    private int avrgSpeed = 0;
    private double distanceValue = 0.0;
    private long startingTime = 0;
    private boolean isTrackStarted = false;
    private RecordingState recordingState = RecordingState.RECORDING_STOPPED;

    // currently visible notification
    private Notification notification;

    /**
     * Constructor.
     *
     */
    public RecordingNotification(RecordingService recordingService, Bus eventBus) {
        this.context = recordingService;
        this.eventBus = eventBus;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.screenClass = BaseMainActivity.class;
        this.channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createChannel() : "";
    }

    /**
     * Handles onCreate events of the bounded observer.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate() {
        this.eventBus.register(this);
        this.refresh();
    }

    /**
     * Handles onDestroy lifecycle events of the bounded observer.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        // unregister from event bus
        try {
            this.eventBus.unregister(this);
        } catch (IllegalArgumentException e) {
            LOG.info("RecordingNotification was not registered on event bus.");
        }

        // cancel notification such that it is not visible anymore.
        this.cancel();
    }

    /**
     * Subscriber method for receiving
     *
     * @param event
     */
    @Subscribe
    public void onReceiveServiceStateChangedEvent(TrackRecordingServiceStateChangedEvent event) {
        this.bluetoothServiceState = event.mState;
        if (event.mState == BluetoothServiceState.SERVICE_STARTED) {

        } else if (event.mState == BluetoothServiceState.SERVICE_STOPPED) {
            this.cancel();
        }
        refresh();
    }

    /**
     * Subscriber method for receiving average speed update events.
     *
     * @param event containing average speed updates.
     */
    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        this.avrgSpeed = event.mAvrgSpeed;
        refresh();
    }

    /**
     * Subscriber method for receiving distance update events.
     *
     * @param event containing distance updates.
     */
    @Subscribe
    public void onReceiveDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        this.distanceValue = event.mDistanceValue;
        refresh();
    }

    /**
     * Subscriber method for receiving starting time events.
     *
     * @param event that contains the starting time.
     */
    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        this.startingTime = event.mStartingTime;
        this.isTrackStarted = event.mIsStarted;
        refresh();
    }

    @Subscribe
    public void onRecordingStateEvent(RecordingStateEvent event) {
        this.recordingState = event.recordingState;
        if (this.recordingState == RecordingState.RECORDING_RUNNING) {
            this.startingTime = SystemClock.elapsedRealtime();
            refresh();
        } else {
            cancel();
        }
    }

    /**
     * Refreshes the notification.
     */
    private synchronized void refresh() {
        if (recordingState == RecordingState.RECORDING_RUNNING) {
            refreshRunning();
        } else {
            refreshStopped();
        }
    }

    private synchronized void refreshStopped() {
        ServiceStateForNotification state ;
        if (recordingState == RecordingState.RECORDING_STOPPED) {
            state = ServiceStateForNotification.UNCONNECTED;
        } else {
            state = ServiceStateForNotification.CONNECTING;
        }

        Intent i = new Intent(context, BaseMainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        this.notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(state.getTitle()))
                .setContentText(context.getString(state.getSubText()))
                .setSmallIcon(state.getIcon())
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        this.context.startForeground(this.notificationId, this.notification);
    }

    private synchronized void refreshRunning() {
        if (recordingState != RecordingState.RECORDING_RUNNING)
            return;

        Intent intent = new Intent(this.context, this.screenClass);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this.context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationActionHolder actionHolder = ServiceStateForNotification.CONNECTED.getAction(this.context);

        // populate big notification layout
        RemoteViews bigLayout = new RemoteViews(context.getPackageName(), R.layout.notification_while_track_recording);
        bigLayout.setOnClickPendingIntent(R.id.notification_obd_service_state_button, actionHolder.actionIntent);
        bigLayout.setTextViewText(R.id.notification_distance, String.format("%s km", DECIMAL_FORMATTER.format(distanceValue)));
        bigLayout.setTextViewText(R.id.notification_speed, String.format("%s km/h", Integer.toString(avrgSpeed)));
        bigLayout.setChronometer(R.id.notification_timertext, startingTime, "%s", isTrackStarted);

        // populate small notification layout
        RemoteViews smallLayout = new RemoteViews(context.getPackageName(), R.layout.notification_while_track_recording_small);
        smallLayout.setTextViewText(R.id.notification_distance, String.format("%s km", DECIMAL_FORMATTER.format(distanceValue)));
        smallLayout.setTextViewText(R.id.notification_speed, String.format("%s km/h", Integer.toString(avrgSpeed)));
        smallLayout.setChronometer(R.id.notification_timertext, startingTime, "%s", isTrackStarted);

        // create new Notification
        this.notification = new NotificationCompat.Builder(this.context, DEFAULT_CHANNEL_ID)
                .setSmallIcon(ServiceStateForNotification.CONNECTED.getIcon())
                .setContentIntent(pIntent)
                .setCustomContentView(smallLayout)
                .setCustomBigContentView(bigLayout)
                .setAutoCancel(true).build();

        // notify change
        this.context.startForeground(notificationId, this.notification);
    }

    /**
     * Deletes the recording notification
     */
    private void cancel() {
        this.notificationManager.cancel(notificationId);
    }

    @TargetApi(26)
    private synchronized String createChannel() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, "recording notification state", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Recording Notification");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        return DEFAULT_CHANNEL_ID;
    }
}
