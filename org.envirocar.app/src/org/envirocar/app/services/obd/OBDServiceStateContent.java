package org.envirocar.app.services.obd;

import android.content.Context;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
interface OBDServiceStateContent {
    int getTitle();
    int getSubText();
    int getIcon();
    OBDNotificationActionHolder getAction(Context context);
}
