package org.envirocar.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;

import com.google.common.collect.Maps;

import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.services.SystemStartupService;

import java.util.Map;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class NotificationHandler {
    private static final Logger LOGGER = Logger.getLogger(NotificationHandler.class);

    // TODO remove this
    private static final int mId = 133;
    private static int NOTIFICATION_ID = 1000;

    /**
     * Returns a new notification id.
     *
     * @return a new notification id.
     */
    private static final int getNotificationID() {
        return NOTIFICATION_ID++;
    }

    // Injected fields.
    @Inject
    @InjectApplicationScope
    protected Context mContext;

    private NotificationManager mNotificationManager;
    private PendingIntent mBaseContentIntent;
    private Map<Class<?>, Integer> mServiceToNotificationID = Maps.newConcurrentMap();

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public NotificationHandler(Context context) {
        // Inject ourselves
        ((Injector) context).injectObjects(this);

        // get the system service for notifications.
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context
                .NOTIFICATION_SERVICE);

        // Set up the pending intent for the Mainactivity
        Intent baseIntent = new Intent(mContext, BaseMainActivity.class);
        mBaseContentIntent = PendingIntent.getActivity(mContext, 0, baseIntent, 0);
    }


    /**
     * TODO REMOVE THIS
     * (Can also contain the http status code with error if fail)
     *
     * @param action
     */
    public void createNotification(String action) {
        String notification_text = "";
        if (action.equals("success")) {
            notification_text = mContext.getString(R.string.upload_notification_success);
        } else if (action.equals("start")) {
            notification_text = mContext.getString(R.string.upload_notification);
        } else {
            notification_text = action;
        }

        Intent intent = new Intent(mContext, BaseMainActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(mContext, 0, intent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("enviroCar")
                        .setContentText(notification_text)
                        .setContentIntent(pintent)
                        .setTicker(notification_text);

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());

    }

    public void setNotificationState(Service service, NotificationState state) {
        int notificationID;
        if (!mServiceToNotificationID.containsKey(service.getClass())) {
            notificationID = getNotificationID();
            mServiceToNotificationID.put(service.getClass(), notificationID);

            // run a dummy notification in the foreground.
            Notification.Builder builder = new Notification.Builder(mContext);
            service.startForeground(notificationID, builder.build());
        } else {
            notificationID = mServiceToNotificationID.get(service.getClass());
        }


        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setContentTitle(state.getNotificationTitle());
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setContentText(state.getNotificationContent());
        builder.setSmallIcon(state.getSmallIconId());

        // TODO
        builder.setLargeIcon(getBitmap(
                mContext.getResources().getDrawable(state.getLargeIconId())));
        builder.setContentIntent(mBaseContentIntent);

        if (state.isShowingBigText()) {
            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            bigTextStyle.setBigContentTitle(state.getNotificationTitle());
            bigTextStyle.bigText(state.getNotificationContent());
            builder.setStyle(bigTextStyle);
        }

        for (NotificationActionHolder holder : state.getActions(mContext)) {
            builder.addAction(holder.mActionIcon, holder.mActionTitle, holder.mPendingIntent);
        }

        mNotificationManager.notify(notificationID, builder.build());
    }

    /**
     * TODO move into a static utils class
     *
     * @param drawable
     * @return
     */
    private Bitmap getBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Closes the notification for a given service.
     *
     * @param service the service for which the notification is required to be closed.
     */
    public void closeNotification(Service service) {
        if (mNotificationManager != null) {
            Integer notificationID = mServiceToNotificationID.get(service.getClass());
            if (notificationID != null)
                mNotificationManager.cancel(notificationID);
        }
    }


    /**
     * Enumeration reflecting the possible states of the application.
     */
    public enum NotificationState implements NotificationContent {
        NO_OBD_SELECTED {
            @Override
            public String getNotificationTitle() {
                return "No OBDII Adapter Selected";
            }

            @Override
            public String getNotificationContent() {
                return "You have no paired OBDII adapter selected. Go to the settings and " +
                        "pair/select one.";
            }

            @Override
            public boolean isShowingBigText() {
                return false;
            }
        },
        UNCONNECTED {
            @Override
            public String getNotificationTitle() {
                return "Device is Unconnected.";
            }

            @Override
            public String getNotificationContent() {
                return "The device is not connected to any OBD device yet.";
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(SystemStartupService.ACTION_START_BT_DISCOVERY);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_bluetooth_searching_black_24dp,
                                "Start Bluetooth Discovery", pendingIntent)};
            }
        },
        DISCOVERING {
            @Override
            public String getNotificationTitle() {
                return "Discovering";
            }

            @Override
            public String getNotificationContent() {
                return "Discovering for other bluetooth devices in the near Environment.";
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public int getSmallIconId() {
                return R.drawable.ic_bluetooth_searching_black_24dp;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(SystemStartupService.ACTION_STOP_BT_DISCOVERY);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_close_black_24dp,
                                "Cancel Discovery", pendingIntent)};
            }
        },
        OBD_FOUND {
            @Override
            public String getNotificationTitle() {
                return "OBD Device in Range.";
            }

            @Override
            public String getNotificationContent() {
                return "The selected OBD device is found. Trying to connect";
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(SystemStartupService.ACTION_START_TRACK_RECORDING);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_play_arrow_black_24dp,
                                "Start Track", pendingIntent)};
            }
        },
        CONNECTING {
            @Override
            public String getNotificationTitle() {
                return "Connecting...";
            }

            @Override
            public String getNotificationContent() {
                return "Connecting and starting a new track";
            }

            @Override
            public boolean isShowingBigText() {
                return false;
            }
        },
        CONNCECTED {
            @Override
            public String getNotificationTitle() {
                return "Recording...";
            }

            @Override
            public String getNotificationContent() {
                return "Successfully connected to the OBD device.\nThe recording is in progress.";
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public int getSmallIconId() {
                return R.drawable.ic_play_arrow_black_24dp;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(SystemStartupService.ACTION_STOP_TRACK_RECORDING);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_stop_black_24dp,
                                "Stop Track", pendingIntent)};
            }
        },
        STOPPING {
            @Override
            public String getNotificationTitle() {
                return "Stopping the Track";
            }

            @Override
            public String getNotificationContent() {
                return "The tracking process gets stopped and finalized.";
            }

            @Override
            public boolean isShowingBigText() {
                return false;
            }
        };


        @Override
        public int getSmallIconId() {
            return android.R.drawable.alert_light_frame;
        }

        @Override
        public int getLargeIconId() {
            return R.drawable.ic_launcher;
        }


        @Override
        public NotificationActionHolder[] getActions(Context context) {
            return new NotificationActionHolder[0];
        }
    }

    /**
     * Interface for the NotificationState enumeration that provides access
     * to the state specific settings.
     */
    private interface NotificationContent {
        /**
         * Returns the string for the notification title.
         *
         * @return the title of the notification.
         */
        String getNotificationTitle();

        /**
         * Returns the string for the notification content.
         *
         * @return the content of the notification.
         */
        String getNotificationContent();

        /**
         * Returns a boolean value that indicates whether the notification is showing a big text.
         *
         * @return true if the notification should be shown in a big text style.
         */
        boolean isShowingBigText();

        /**
         * Returns the id of the small icon to be shown in the notification.
         *
         * @return id of the small icon.
         */
        int getSmallIconId();

        /**
         * Return the id of the large icon to be shown in the notification.
         *
         * @return id of the large icon.
         */
        int getLargeIconId();

        /**
         * Returns the possible interactions to be visualized in the notification bar as
         * NotificaitonActionHolder.
         *
         * @return the actions to be added to the notification
         */
        NotificationActionHolder[] getActions(Context context);
    }

    /**
     * Holder class that holds the action specific details for notifications. Using Notification
     * .Action is not possible, because the add method is only accessible for android versions
     * >20. Therefore, this holder class is a workaround to allow individual actions for the
     * notifications.
     */
    private static final class NotificationActionHolder {
        int mActionIcon;
        String mActionTitle;
        PendingIntent mPendingIntent;

        /**
         * Constructor.
         *
         * @param actionIcon  The icon of the action.
         * @param actionTitle The title of the action.
         * @param intent      The pending intent of the action.
         */
        NotificationActionHolder(int actionIcon, String actionTitle, PendingIntent intent) {
            this.mActionIcon = actionIcon;
            this.mActionTitle = actionTitle;
            this.mPendingIntent = intent;
        }
    }
}
