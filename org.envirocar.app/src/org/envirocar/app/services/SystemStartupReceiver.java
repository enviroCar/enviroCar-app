package org.envirocar.app.services;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.preferences.PreferenceConstants;

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

            // If bluetooth is enabled, then start the background service.
            if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                startSystemStartupService(context);

        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            LOGGER.info("Received BluetoothAdapter.ACTION_STATE_CHANGED broadcast.");

            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    // If bluetooth has been turned on, then check wheterh the background service
                    // needs to be started.
                    startSystemStartupService(context);
                    break;
            }
        }
    }

    /**
     * Starts the SystemStartupService if the preference is setted and the service is not already
     * running.
     *
     * @param context the context of the current scope.
     */
    private void startSystemStartupService(Context context) {
        // Get the preference related to the autoconnection.
        boolean autoStartService = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferenceConstants.
                        PREFERENCE_TAG_BLUETOOTH_SERVICE_AUTOSTART, false);

        // If autostart service is on and the service is not already running,
        // then start the background service.
        if (autoStartService && !ServiceUtils.isServiceRunning(
                context, SystemStartupService.class)) {
            Intent startIntent = new Intent(context, SystemStartupService.class);
            context.startService(startIntent);
        }
    }

}
