package org.envirocar.app.services.obd;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import org.envirocar.app.R;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class OBDServiceHandler {
    private static final Logger LOG = Logger.getLogger(OBDServiceHandler.class);

    @Inject
    @InjectApplicationScope
    public static Context context;

    private static Notification foregroundNotification;
    private static RemoteViews smallView;
    private static RemoteViews bigView;
    private static OBDServiceState obdServiceState = OBDServiceState.UNCONNECTED;

    @Inject
    public OBDServiceHandler(@InjectApplicationScope Context context) {
        //        this.context = context;
        //
        //        this.notificationManager = (NotificationManager) context.getSystemService(Context
        //                .NOTIFICATION_SERVICE);
    }

    public static void closeNotification(){
        ((NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE)).cancel(1991);
    }

    public static OBDServiceState getRecordingState(){
        return obdServiceState;
    }

    public static void setRecordingState(OBDServiceState state) {
        obdServiceState = state;

        // Set the settings and views for the small notification view.
        smallView = new RemoteViews(context.getPackageName(), R.layout
                .notification_obd_service_state);
        setSmallViewText(state.getTitle(), state.getSubText());

        foregroundNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(state.getIcon())
                .setContentTitle(context.getString(state.getTitle()))
                .setPriority(Integer.MAX_VALUE)
                .setContent(smallView)
                .build();

        // Check whether the notification state has content for the bigView, i.e. it provides an
        // notification action holder.
        OBDNotificationActionHolder actionHolder = state.getAction(context);
        if (actionHolder != null) {
            bigView = new RemoteViews(context.getPackageName(), R.layout
                    .notification_obd_service_state_big);
            setBigViewText(state.getTitle(), state.getSubText(), state.getAction(context));
            foregroundNotification.bigContentView = bigView;
        }

        // Finally, notify the notificationmanager to update the notification view.
        ((NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE)).notify(1991, foregroundNotification);
    }

    private static void setSmallViewText(int title, int summary) {
        setSmallViewText(context.getString(title), context.getString(summary));
    }

    private static void setSmallViewText(String title, String summary) {
        smallView.setTextViewText(R.id.notification_obd_service_state_title, title);
        smallView.setTextViewText(R.id.notification_obd_service_state_summary, summary);
    }

    private static void setBigViewText(int title, int summary, OBDNotificationActionHolder holder) {
        setBigViewText(context.getString(title), context.getString(summary), holder);
    }

    private static void setBigViewText(String title, String summary, OBDNotificationActionHolder
            holder) {
        bigView.setTextViewText(R.id.notification_obd_service_state_title, title);
        bigView.setTextViewText(R.id.notification_obd_service_state_summary, summary);

        if (holder != null) {
            bigView.setInt(R.id.notification_obd_service_state_button_img,
                    "setImageResource", holder.actionIcon);
            bigView.setInt(R.id.notification_obd_service_state_button_text,
                    "setText", holder.actionString);
            bigView.setOnClickPendingIntent(R.id.notification_obd_service_state_button,
                    holder.actionIntent);
        }
    }
}
