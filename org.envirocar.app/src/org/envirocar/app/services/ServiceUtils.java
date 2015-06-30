package org.envirocar.app.services;

import android.app.ActivityManager;
import android.content.Context;

/**
 * @author dewall
 */
public final class ServiceUtils {

    /**
     * Checks whether there is a specific service already running and, if so, it returns true.
     *
     * @param context      the context of the current scope (e.g. application scope)
     * @param serviceClass the class of the service to check its state.
     * @return true if there is a running instance of the given service class.
     */
    public static final boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context
                .ACTIVITY_SERVICE);

        // Check for each service of this application whether it is the service to search for.
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer
                .MAX_VALUE)) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
