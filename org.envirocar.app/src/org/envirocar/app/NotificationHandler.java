package org.envirocar.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;


import javax.inject.Inject;

/**
 * Created by Peter on 02.06.2015.
 */
public class NotificationHandler {

    private int mId = 1133;

    @Inject
    protected Context mContext;

    public NotificationHandler(Context context){
        ((BaseApplication) context).injectObjects(this);
    }

    /**
     * @action Can also contain the http status code with error if fail
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
}
