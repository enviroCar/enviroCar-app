package org.envirocar.app.services;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.envirocar.app.logging.Logger;

/**
 * Startup receiver that listens to ACTION_BOOT_COMPLETED broadcasts and therefore starts when
 * the device has been successfully booted. This receiver starts the general background service of
 * the mobile application.
 *
 * @author dewall
 */
public class SystemStartupReceiver extends BroadcastReceiver{
    private static final Logger LOGGER = Logger.getLogger(SystemStartupReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.info("Received intent broadcast");
        String action = intent.getAction();

        // If the device completes his boot process
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)){
            LOGGER.info("Received ACTION_BOOT_COMPLETED broadcast.");
            // Start a new service
            if(!isServiceRunning(SystemStartupService.class, context)) {
                Intent startIntent = new Intent(context, SystemStartupService.class);
                context.startService(startIntent);
            }
        }
    }

    /**
     * @param serviceClass
     * @return
     */
    private boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context
                .ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer
                .MAX_VALUE)) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
