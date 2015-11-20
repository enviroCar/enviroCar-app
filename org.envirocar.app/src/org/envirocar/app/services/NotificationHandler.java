/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.services;

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

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.R;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;

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
    private Map<Class<?>, NotificationState> mNotificationStateMap = Maps.newConcurrentMap();

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public NotificationHandler(Context context) {
        // Inject ourselves
        ((Injector) context).injectObjects(this);

        // get the system remoteService for notifications.
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
        LOGGER.info(String.format("setNotificationState(state=%s)", state));
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

        mNotificationStateMap.put(service.getClass(), state);

        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setContentTitle("enviroCar - " + mContext.getString(state.getNotificationTitle()));
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setContentText(mContext.getString(state.getNotificationContent()));
        builder.setSmallIcon(state.getSmallIconId());

        // TODO
        //        builder.setLargeIcon(getBitmap(
        //                mContext.getResources().getDrawable(state.getLargeIconId())));
        builder.setContentIntent(mBaseContentIntent);

        if (state.isShowingBigText()) {
            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            bigTextStyle.setBigContentTitle("enviroCar - " + mContext.getString(state.getNotificationTitle()));
            bigTextStyle.bigText(mContext.getString(state.getNotificationContent()));
            builder.setStyle(bigTextStyle);
        }

        for (NotificationActionHolder holder : state.getActions(mContext)) {
            builder.addAction(holder.mActionIcon, mContext.getString(holder.mActionTitle), holder.mPendingIntent);
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
     * Closes the notification for a given remoteService.
     *
     * @param service the remoteService for which the notification is required to be closed.
     */
    public void closeNotification(Service service) {
        if (mNotificationManager != null) {
            Integer notificationID = mServiceToNotificationID.get(service.getClass());
            if (notificationID != null)
                mNotificationManager.cancel(notificationID);
        }
    }

    public NotificationState getCurrentNotificationState(Service service) {
        return mNotificationStateMap.get(service.getClass());
    }


    /**
     * Enumeration reflecting the possible states of the application.
     */
    public enum NotificationState implements NotificationContent {
        NO_CAR_SELECTED {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_no_car_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_no_car_description;
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(context, CarSelectionActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_drive_eta_black_24dp,
                                R.string.notification_no_car_action, pendingIntent)};
            }
        },
        NO_OBD_SELECTED {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_no_obd_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_no_obd_description;
            }

            @Override
            public boolean isShowingBigText() {
                return true;
            }

            @Override
            public NotificationActionHolder[] getActions(Context context) {
                Intent intent = new Intent(context, OBDSelectionActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                return new NotificationActionHolder[]{
                        new NotificationActionHolder(R.drawable.ic_bluetooth_black_24dp,
                                R.string.notification_no_obd_action, pendingIntent)};
            }
        },
        UNCONNECTED {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_unconnected_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_unconnected_description;
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
                                R.string.notification_unconnected_action, pendingIntent)};
            }
        },
        DISCOVERING {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_discovering_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_discovering_description;
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
                                R.string.notification_discovering_action, pendingIntent)};
            }
        },
        OBD_FOUND {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_obd_found_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_obd_found_description;
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
                                R.string.notification_obd_found_action, pendingIntent)};
            }
        },
        CONNECTING {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_connecting_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_connecting_description;
            }

            @Override
            public boolean isShowingBigText() {
                return false;
            }
        },
        CONNCECTED {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_connected_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_connected_description;
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
                                R.string.notification_connected_action, pendingIntent)};
            }
        },
        STOPPING {
            @Override
            public int getNotificationTitle() {
                return R.string.notification_stopping_title;
            }

            @Override
            public int getNotificationContent() {
                return R.string.notification_stopping_description;
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
        int getNotificationTitle();

        /**
         * Returns the string for the notification content.
         *
         * @return the content of the notification.
         */
        int getNotificationContent();

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
        int mActionTitle;
        PendingIntent mPendingIntent;

        /**
         * Constructor.
         *
         * @param actionIcon  The icon of the action.
         * @param actionTitle The title of the action.
         * @param intent      The pending intent of the action.
         */
        NotificationActionHolder(int actionIcon, int actionTitle, PendingIntent intent) {
            this.mActionIcon = actionIcon;
            this.mActionTitle = actionTitle;
            this.mPendingIntent = intent;
        }
    }
}
