package org.envirocar.app.services.obd;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.RemoteViews;

import org.envirocar.app.BaseMainActivityBottomBar;
import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.InjectApplicationScope;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class OBDServiceHandler {
    private static final Logger LOG = Logger.getLogger(OBDServiceHandler.class);


    public static Context context;

    private static Notification foregroundNotification;
    private static RemoteViews smallView;
    private static RemoteViews bigView;
    private static OBDServiceState obdServiceState = OBDServiceState.UNCONNECTED;

    private static String CHANNEL_ID = "channel1";

    @Inject
    public OBDServiceHandler(@InjectApplicationScope Context context) {
               // OBDServiceHandler.context = context;
        //
        //        this.notificationManager = (NotificationManager) context.getSystemService(Context
        //                .NOTIFICATION_SERVICE);
    }

    public static void closeNotification(){
        ((NotificationManager) context.getSystemService(
                NOTIFICATION_SERVICE)).cancel(1991);
    }

    public static OBDServiceState getRecordingState(){
        return obdServiceState;
    }

    public static void setRecordingState(OBDServiceState state) {
        obdServiceState = state;

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n;

        Intent intent = new Intent(context, BaseMainActivityBottomBar.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);


        OBDNotificationActionHolder actionHolder = state.getAction(context);
        if (actionHolder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification.Action action = new Notification.Action.Builder(Icon.createWithResource(context,actionHolder.actionIcon),
                        context.getString(actionHolder.actionString),
                        actionHolder.actionIntent).build();


                n  = new Notification.Builder(context, CHANNEL_ID)
                        .setContentTitle(context.getString(state.getTitle()))
                        .setContentText(context.getString(state.getSubText()))
                        .setSmallIcon(state.getIcon())
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .addAction(action)
                        .build();

            }else{
                n  = new Notification.Builder(context)
                        .setContentTitle(context.getString(state.getTitle()))
                        .setContentText(context.getString(state.getSubText()))
                        .setSmallIcon(state.getIcon())
                        .setContentIntent(pIntent)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .addAction(actionHolder.actionIcon, context.getString(actionHolder.actionString), actionHolder.actionIntent)
                        .build();
            }

        }
        else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                n  = new Notification.Builder(context, CHANNEL_ID)
                        .setContentTitle(context.getString(state.getTitle()))
                        .setContentText(context.getString(state.getSubText()))
                        .setSmallIcon(state.getIcon())
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .build();

            }else{
                n  = new Notification.Builder(context)
                        .setContentTitle(context.getString(state.getTitle()))
                        .setContentText(context.getString(state.getSubText()))
                        .setSmallIcon(state.getIcon())
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .build();
            }
        }


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        n.flags = Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(1991, n);

       /* // Set the settings and views for the small notification view.
        smallView = new RemoteViews(context.getPackageName(), R.layout
                .notification_obd_service_state);
        setSmallViewText(state.getTitle(), state.getSubText());

        foregroundNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(state.getIcon())
                .setContentTitle(context.getString(state.getTitle()))
                .setPriority(Integer.MAX_VALUE)
                .setContent(smallView)
                .setOngoing(true)
                .setAutoCancel(true)
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
                NOTIFICATION_SERVICE)).notify(1991, foregroundNotification);*/
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
