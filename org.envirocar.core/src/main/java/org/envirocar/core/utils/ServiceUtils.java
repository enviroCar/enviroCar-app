package org.envirocar.core.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * @author dewall
 */
public final class ServiceUtils {

    /**
     * Checks whether there is a specific remoteService already running and, if so, it returns true.
     *
     * @param context      the context of the current scope (e.g. application scope)
     * @param serviceClass the class of the remoteService to check its state.
     * @return true if there is a running instance of the given remoteService class.
     */
    public static final boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context
                .ACTIVITY_SERVICE);

        // Check for each remoteService of this application whether it is the remoteService to search for.
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer
                .MAX_VALUE)) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
