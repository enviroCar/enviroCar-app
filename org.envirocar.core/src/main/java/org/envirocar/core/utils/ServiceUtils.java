/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.envirocar.core.logging.Logger;

/**
 * @author dewall
 */
public final class ServiceUtils {
    private static final Logger LOGGER = Logger.getLogger(ServiceUtils.class);

    public static final void startService(Context context, Class<?> service){
        LOGGER.info("Trying to start %s", service.getSimpleName());

        // If the service is not already running, the start the service.
        if(!ServiceUtils.isServiceRunning(context, service)) {
            Intent startIntent = new Intent(context, service);
            ContextCompat.startForegroundService(context, startIntent);
            LOGGER.info("%s successfuly started.", service.getSimpleName());
        } else {
            LOGGER.info("%s was already running. No start required!", service.getSimpleName());
        }
    }

    public static final void stopService(Context context, Class<?> service){
        LOGGER.info("Trying to stop %s", service.getSimpleName());

        // If the service is already running, then stopo the service.
        if(ServiceUtils.isServiceRunning(context, service)){
            Intent stopIntent = new Intent(context, service);
            context.stopService(stopIntent);
            LOGGER.info("%s successfully stopped.", service.getSimpleName());
        } else {
            LOGGER.info("%s is not running. No stop required!", service.getSimpleName());
        }
    }

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
