package org.envirocar.app.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.envirocar.app.R;
import org.envirocar.app.services.SystemStartupService;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public enum ServiceStateForNotificationForNotification implements ServiceStateContentForNotification {
    NO_CAR_SELECTED {
        @Override
        public int getTitle() {
            return R.string.notification_no_car_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_no_car_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    NO_OBD_SELECTED {
        @Override
        public int getTitle() {
            return R.string.notification_no_obd_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_no_obd_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    UNCONNECTED {
        @Override
        public int getTitle() {
            return R.string.notification_unconnected_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_unconnected_description;
        }

        @Override
        public int getIcon() {
            return R.drawable.av_stop;
        }

        @Override
        public NotificationActionHolder getAction(Context context) {
            return new NotificationActionHolder(
                    R.drawable.ic_bluetooth_searching_black_24dp,
                    R.string.notification_unconnected_action,
                    getPendingIntent(SystemStartupService.ACTION_START_BT_DISCOVERY, context));
        }
    },
    DISCOVERING {
        @Override
        public int getTitle() {
            return R.string.notification_discovering_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_discovering_description;
        }

        @Override
        public int getIcon() {
            return R.drawable.ic_bluetooth_searching_white_24dp;
        }

        @Override
        public NotificationActionHolder getAction(Context context) {
            return new NotificationActionHolder(
                    R.drawable.ic_close_black_24dp,
                    R.string.notification_discovering_action,
                    getPendingIntent(SystemStartupService.ACTION_STOP_BT_DISCOVERY, context));
        }
    },
    CONNECTING {
        @Override
        public int getTitle() {
            return R.string.notification_connecting_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_connecting_description;
        }

        @Override
        public int getIcon() {
            return R.drawable.ic_bluetooth_searching_white_24dp;
        }
    },
    CONNECTED {
        @Override
        public int getTitle() {
            return R.string.notification_connected_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_connected_description;
        }

        @Override
        public int getIcon() {
            return R.drawable.ic_play_arrow_black_24dp;
        }

        @Override
        public NotificationActionHolder getAction(Context context) {
            return new NotificationActionHolder(
                    R.drawable.ic_close_black_24dp,
                    R.string.notification_connected_action,
                    getPendingIntent(SystemStartupService.ACTION_STOP_TRACK_RECORDING, context));
        }
    },
    STOPPING {
        @Override
        public int getTitle() {
            return R.string.notification_stopping_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_stopping_description;
        }

        @Override
        public int getIcon() {
            return R.drawable.ic_bluetooth_disabled_black_24dp;
        }
    };

    @Override
    public int getTitle() {
        return 0;
    }

    @Override
    public int getSubText() {
        return 0;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_bluetooth_black_24dp;
    }

    @Override
    public NotificationActionHolder getAction(Context context) {
        return null;
    }

    protected PendingIntent getPendingIntent(String broadcastAction, Context context) {
        Intent intent = new Intent(broadcastAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
}
