package org.envirocar.app.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.RemoteViews;

import org.envirocar.app.main.BaseMainActivityBottomBar;
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
public class NotificationHandler {
    private static final Logger LOG = Logger.getLogger(NotificationHandler.class);


    public static Context context;

    private static Notification foregroundNotification;
    private static RemoteViews smallView;
    private static RemoteViews bigView;
    private static ServiceStateForNotificationForNotification serviceStateForNotification = ServiceStateForNotificationForNotification.UNCONNECTED;

    private static String CHANNEL_ID = "channel1";

    @Inject
    public NotificationHandler(@InjectApplicationScope Context context) {
               // NotificationHandler.context = context;
        //
        //        this.notificationManager = (NotificationManager) context.getSystemService(Context
        //                .NOTIFICATION_SERVICE);
    }

    public static void closeNotification(){
        ((NotificationManager) context.getSystemService(
                NOTIFICATION_SERVICE)).cancel(1991);
    }

    public static ServiceStateForNotificationForNotification getRecordingState(){
        return serviceStateForNotification;
    }

    public static void setRecordingState(ServiceStateForNotificationForNotification state) {
        serviceStateForNotification = state;

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n;

        Intent intent = new Intent(context, BaseMainActivityBottomBar.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);


        NotificationActionHolder actionHolder = state.getAction(context);
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

    }

}
