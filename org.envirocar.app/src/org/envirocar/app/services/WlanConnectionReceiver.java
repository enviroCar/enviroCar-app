package org.envirocar.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

/**
 * @author dewall
 */
public class WlanConnectionReceiver extends BroadcastReceiver {
    private static final Logger LOG = Logger.getLogger(WlanConnectionReceiver.class);

    private static Boolean IS_FIRST_CONNECT = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean connected = info.isConnected();

            NetworkInfo.State state = info.getState();
            if (connected) {
                if (IS_FIRST_CONNECT) {
                    IS_FIRST_CONNECT = false;
                    LOG.info("Is connected to wlan.");

                    // Only start the track upload when the specific settings have been made.
                    if (checkUploadTracks(context)) {
                        startUploadTracksService(context);
                    }
                }
            } else if(!IS_FIRST_CONNECT){
                stopUploadTracksService(context);
                IS_FIRST_CONNECT = true;
            }
        }
    }

    /**
     * Checks whether the trackupload
     *
     * @param context
     * @return
     */
    private boolean checkUploadTracks(Context context) {
        LOG.info("Check whether tracks have to be uploaded.");

        // Check whether the automatic upload of tracks within WLAN is active
        boolean autoUploadTracks = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferenceConstants.ALWAYS_UPLOAD, false);
        if (autoUploadTracks) {
            LOG.info("Automatic track upload is enabled");
            // Check if there are some local tracks in the databse
            //            DbAdapter dbAdapter = ((Injector) context.getApplicationContext())
            // .getObjectGraph().get(DbAdapter.class);
            //            if (dbAdapter != null && !dbAdapter.getAllLocalTracks(true).isEmpty()) {
            //                LOG.info("Automatic track upload is checked and " +
            //                        "there are some local tracks to upload.");
            //                return true;
            //            }
        }

        return autoUploadTracks;
    }

    /**
     * Starts the track upload background remoteService when currently inactive.
     *
     * @param context the context of the current scope.
     */
    private void startUploadTracksService(Context context) {
        LOG.info("startUploadTracksService()");
        if (!ServiceUtils.isServiceRunning(context, TrackUploadService.class)) {
            context.startService(new Intent(context, TrackUploadService.class));
            LOG.info("Successfully started the track upload remoteService.");
        }
    }

    /**
     * Stops the track upload background remoteService when currently active.
     *
     * @param context the context of the current scope.
     */
    private void stopUploadTracksService(Context context) {
        LOG.info("stopIploadTracksService()");
        if (ServiceUtils.isServiceRunning(context, TrackUploadService.class)) {
            context.stopService(new Intent(context, TrackUploadService.class));
            LOG.info("Successfully stopped the track upload remoteService.");
        }
    }

    /**
     * @param context
     * @return
     */
    private boolean isConnectedViaWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService
                (Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }
}
