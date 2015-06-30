package org.envirocar.app.services;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.envirocar.app.logging.Logger;

/**
 * Startup receiver that listens to ACTION_BOOT_COMPLETED broadcasts and therefore starts when
 * the device has been successfully booted. This receiver starts the general background service of
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

            // Start a new service
            if (!ServiceUtils.isServiceRunning(context, SystemStartupService.class)) {
                Intent startIntent = new Intent(context, SystemStartupService.class);
                context.startService(startIntent);
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            LOGGER.info("Received BluetoothAdapter.ACTION_STATE_CHANGED broadcast.");

            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            switch (state){
                case BluetoothAdapter.STATE_ON:
                    // If the service is not already runningy, then start the startup service.
                    if (!ServiceUtils.isServiceRunning(context, SystemStartupService.class)) {
                        Intent startIntent = new Intent(context, SystemStartupService.class);
                        context.startService(startIntent);
                    }
                    break;
            }
        }
    }

}
