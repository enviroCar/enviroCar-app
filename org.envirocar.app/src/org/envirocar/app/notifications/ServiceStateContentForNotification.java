package org.envirocar.app.notifications;

import android.content.Context;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
interface ServiceStateContentForNotification {
    int getTitle();
    int getSubText();
    int getIcon();
    NotificationActionHolder getAction(Context context);
}
