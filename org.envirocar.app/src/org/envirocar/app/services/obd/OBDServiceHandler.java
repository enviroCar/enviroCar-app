package org.envirocar.app.services.obd;

import android.content.Context;

import org.envirocar.app.services.NotificationHandler;
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
    private static final Logger LOGGER = Logger.getLogger(NotificationHandler.class);

    private static final int mId = 133;
    private static final int NOTIFICATION_ID = 1000;

    private final Context context;

    @Inject
    public OBDServiceHandler(Context context){
        this.context = context;
    }

    public void setRecordingState(NotificationHandler.NotificationState state){

    }
}
