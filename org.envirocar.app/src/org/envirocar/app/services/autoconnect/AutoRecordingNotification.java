package org.envirocar.app.services.autoconnect;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;

import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.recording.notification.RecordingNotification;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.core.logging.Logger;

/**
 * @author dewall
 */
public class AutoRecordingNotification implements LifecycleObserver {
    private static final Logger LOG = Logger.getLogger(RecordingNotification.class);

    private static final String DEFAULT_CHANNEL_ID = "com.envirocar.app.services.autoconnect.notification";
    private static final int NOTIFICATION_ID = 259;

    private final AutoRecordingService recordingService;
    private final Bus eventBus;
    private final BluetoothHandler bluetoothHandler;
    private final NotificationManager notificationManager;
    private final String channelId;

    private Notification notification;

    /**
     * Constructor.
     *
     * @param recordingService
     * @param eventBus
     */
    public AutoRecordingNotification(AutoRecordingService recordingService, Bus eventBus, BluetoothHandler bluetoothHandler) {
        this.recordingService = recordingService;
        this.eventBus = eventBus;
        this.notificationManager = (NotificationManager) recordingService.getSystemService(Context.NOTIFICATION_SERVICE);
        this.bluetoothHandler = bluetoothHandler;
        this.channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createChannel() : "";
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        this.cancel();
    }

    protected synchronized void setAutoRecordingState(RecordingType recordingType, AutoRecordingStrategy.AutoRecordingState state) {
        Intent intent = new Intent(recordingService, BaseMainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(recordingService, (int) System.currentTimeMillis(), intent, 0);

        String contentText = recordingService.getString(state.getSubTextRes());
        BluetoothDevice device = bluetoothHandler.getSelectedBluetoothDevice();
        if (state == AutoRecordingStrategy.AutoRecordingState.ACTIVE &&
                recordingType == RecordingType.OBD_ADAPTER_BASED &&
                device != null) {
            contentText = String.format(contentText, device.getName());
        }

        this.notification = new NotificationCompat.Builder(recordingService, channelId)
                .setContentTitle(recordingService.getString(state.getTitleRes()))
                .setContentText(contentText)
                .setSmallIcon(state.getIconRes())
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

        this.recordingService.startForeground(NOTIFICATION_ID, this.notification);
    }

    /**
     * Deletes the notification.
     */
    private synchronized void cancel() {
        this.notificationManager.cancel(NOTIFICATION_ID);
    }

    @TargetApi(26)
    private synchronized String createChannel() {
        NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, "Autorecording notification state", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Autorecording Notification");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        return DEFAULT_CHANNEL_ID;
    }
}
