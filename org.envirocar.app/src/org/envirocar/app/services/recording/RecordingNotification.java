package org.envirocar.app.services.recording;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.notifications.NotificationActionHolder;
import org.envirocar.app.notifications.ServiceStateForNotification;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RecordingNotification {
    private static final Logger LOG = Logger.getLogger(RecordingNotification.class);
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    // Channel_ID required for newer version
    protected static final String CHANNEL_ID = "channel1";

    // context information
    private final Context context;
    private final Class screenClass;
    private final NotificationManager notificationManager;

    // Stats of the recording
    private BluetoothServiceState bluetoothServiceState;
    private int avrgSpeed = 0;
    private double distanceValue = 0.0;
    private long startingTime = 0;
    private boolean isTrackStarted = false;

    // currently visible notification
    private Notification notification;

    /**
     * Constructor.
     *
     * @param context
     */
    public RecordingNotification(final Context context, Class screenClass, NotificationManager notificationManager) {
        this.context = context;
        this.screenClass = screenClass;
        this.notificationManager = notificationManager;
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
            this.startingTime = SystemClock.elapsedRealtime();
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

    /**
     * Refreshes the notification.
     */
    private void refresh() {
        Intent intent = new Intent(this.context, this.screenClass);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this.context, (int) System.currentTimeMillis(), intent, 0);

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
        this.notification = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                .setSmallIcon(ServiceStateForNotification.CONNECTED.getIcon())
                .setContentIntent(pIntent)
                .setCustomContentView(smallLayout)
                .setCustomBigContentView(bigLayout)
                .setAutoCancel(true).build();

        // notify change
        this.notificationManager.notify(181, this.notification);
    }
}
