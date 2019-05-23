/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

/**
 * Startup receiver that listens to ACTION_BOOT_COMPLETED broadcasts and therefore starts when
 * the device has been successfully booted. This receiver starts the general background remoteService of
 * the mobile application.
 *
 * @author dewall
 */
public class SystemStartupReceiver extends BroadcastReceiver {
    private static final Logger LOGGER = Logger.getLogger(SystemStartupReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.info("Received intent broadcast");
        String action = intent.getAction();

        // If the device completes his boot process
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            LOGGER.info("Received ACTION_BOOT_COMPLETED broadcast.");
            startAutomaticTrackRecordingService(context);
        }
    }

    /**
     * Starts the AutomaticTrackRecordingService if the preference is setted and the remoteService is not already
     * running.
     *
     * @param context the context of the current scope.
     */
    public void startAutomaticTrackRecordingService(Context context) {
        // Get the preference related to the autoconnection.
        boolean autoStartService = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferenceConstants.PREF_BLUETOOTH_SERVICE_AUTOSTART, false);

        // If autostart remoteService is on and the remoteService is not already running,
        // then start the background remoteService.
        if (autoStartService && !ServiceUtils.isServiceRunning(
                context, AutomaticTrackRecordingService.class)) {
            Intent startIntent = new Intent(context, AutomaticTrackRecordingService.class);
            context.startService(startIntent);
        }else if(!autoStartService){
            AutomaticTrackRecordingService.stopService(context);
        }
    }

}
