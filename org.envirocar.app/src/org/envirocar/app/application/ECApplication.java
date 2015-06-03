///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//
//package org.envirocar.app.application;
//
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.app.Application;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.bluetooth.BluetoothAdapter;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import android.os.Build;
//import android.preference.PreferenceManager;
//import android.support.v4.app.NotificationCompat;
//
//import com.google.common.base.Preconditions;
//
//import org.acra.ACRA;
//import org.acra.annotation.ReportsCrashes;
//import org.envirocar.app.FeatureFlags;
//import org.envirocar.app.Injector;
//import org.envirocar.app.R;
//import org.envirocar.app.activity.MainActivity;
//import org.envirocar.app.activity.SettingsActivity;
//import org.envirocar.app.dao.CacheDirectoryProvider;
//import org.envirocar.app.dao.DAOProvider;
//import org.envirocar.app.logging.ACRACustomSender;
//import org.envirocar.app.logging.Logger;
//import org.envirocar.app.storage.DbAdapterImpl;
//import org.envirocar.app.storage.Track;
//import org.envirocar.app.util.Util;
//
//import java.io.File;
//
//import dagger.ObjectGraph;
//import de.keyboardsurfer.android.widget.crouton.Crouton;
//import de.keyboardsurfer.android.widget.crouton.Style;
//
///**
// * This is the main application that is the central linking component for all adapters, services and so on.
// * This application is implemented like a singleton, it exists only once while the app is running.
// */
//@ReportsCrashes(formKey = "")
//public class ECApplication extends Application implements Injector {
//
//    public static final String BASE_URL = "https://envirocar.org/api/stable";
//    private static final Logger logger = Logger.getLogger(ECApplication.class);
//    protected ObjectGraph mObjectGraph;
//    //	public static final String BASE_URL = "http://192.168.1.148:8080/webapp-1.1.0-SNAPSHOT";
//    protected boolean adapterConnected;
//    private SharedPreferences preferences = null;
//    //
////	private final ScheduledExecutorService scheduleTaskExecutor = Executors
////			.newScheduledThreadPool(1, new NamedThreadFactory("ECApplication-Factory"));
//    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter
//            .getDefaultAdapter();
//    private int mId = 1133;
//    private Activity currentActivity;
//
//    private OnSharedPreferenceChangeListener preferenceListener = new OnSharedPreferenceChangeListener() {
//
//        @Override
//        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
//                                              String key) {
//            if (SettingsActivity.ENABLE_DEBUG_LOGGING.equals(key)) {
//                Logger.initialize(Util.getVersionString(ECApplication.this),
//                        sharedPreferences.getBoolean(SettingsActivity.ENABLE_DEBUG_LOGGING, false));
//            }
//        }
//    };
//
//
//    /**
//     * returns the current activity.
//     *
//     * @return
//     */
//    public Activity getCurrentActivity() {
//        return currentActivity;
//    }
//
//
//    public void setActivity(Activity a) {
//        this.currentActivity = a;
//    }
//
//    /**
//     * Returns whether requirements were fulfilled (bluetooth activated)
//     *
//     * @return requirementsFulfilled?
//     */
//    public boolean bluetoothActivated() {
//        if (bluetoothAdapter == null) {
//            logger.warn("Bluetooth disabled");
//            return false;
//        } else {
//            logger.info("Bluetooth enabled");
//            return bluetoothAdapter.isEnabled();
//        }
//    }
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
//
//        Logger.initialize(Util.getVersionString(this),
//                preferences.getBoolean(SettingsActivity.ENABLE_DEBUG_LOGGING, false));
//
//        try {
//            DbAdapterImpl.init(getApplicationContext());
//        } catch (InstantiationException e) {
//            logger.warn("Could not initalize the database layer. The app will probably work unstable.");
//            logger.warn(e.getMessage(), e);
//        }
//
//        DAOProvider.init(new ContextInternetAccessProvider(getApplicationContext()),
//                new CacheDirectoryProvider() {
//                    @Override
//                    public File getBaseFolder() {
//                        return Util.resolveCacheFolder(getApplicationContext());
//                    }
//                });
//
//        UserManager.init(getApplicationContext());
//
//        FeatureFlags.init(getApplicationContext());
//
//        TemporaryFileManager.init(getApplicationContext());
//
//        initializeErrorHandling();
//        CarManager.init(preferences);
//        TermsOfUseManager.instance();
//
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//
//        logger.info("onLowMemory called");
//    }
//
//    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
//    @Override
//    public void onTrimMemory(int level) {
//        super.onTrimMemory(level);
//
//        logger.info("onTrimMemory called");
//
//        logger.info("maxMemory: " + Runtime.getRuntime().maxMemory());
//        logger.info("totalMemory: " + Runtime.getRuntime().totalMemory());
//        logger.info("freeMemory: " + Runtime.getRuntime().freeMemory());
//    }
//
//    private void initializeErrorHandling() {
//        ACRA.init(this);
//        ACRACustomSender yourSender = new ACRACustomSender();
//        ACRA.getErrorReporter().setReportSender(yourSender);
//        ACRA.getConfig().setExcludeMatchingSharedPreferencesKeys(SettingsActivity.resolveIndividualKeys());
//    }
//
////    /**
////     * Stop the service connector and therefore the scheduled tasks.
////     */
////	public void shutdownServiceConnector() {
////		scheduleTaskExecutor.shutdown();
////	}
//
//
//    /**
//     * @action Can also contain the http status code with error if fail
//     */
//    public void createNotification(String action) {
//        String notification_text = "";
//        if (action.equals("success")) {
//            notification_text = getString(R.string.upload_notification_success);
//        } else if (action.equals("start")) {
//            notification_text = getString(R.string.upload_notification);
//        } else {
//            notification_text = action;
//        }
//
//        Intent intent = new Intent(this, MainActivity.class);
//        PendingIntent pintent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
//
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.ic_launcher)
//                        .setContentTitle("enviroCar")
//                        .setContentText(notification_text)
//                        .setContentIntent(pintent)
//                        .setTicker(notification_text);
//
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // mId allows you to update the notification later on.
//        mNotificationManager.notify(mId, mBuilder.build());
//
//    }
//
//    public void finishTrack() {
//        final Track track = DbAdapterImpl.instance().finishCurrentTrack();
//
//        getCurrentActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (track != null) {
//                    if (track.getLastMeasurement() == null) {
//                        Crouton.makeText(getCurrentActivity(), R.string.track_finished_no_measurements, Style.ALERT).show();
//                    } else {
//                        String text = getString(R.string.track_finished).concat(track.getName());
//                        Crouton.makeText(getCurrentActivity(), text, Style.INFO).show();
//                    }
//                } else {
//                    Crouton.makeText(getCurrentActivity(), R.string.track_finishing_failed, Style.ALERT).show();
//                }
//            }
//        });
//    }
//
//
//    @Override
//    public ObjectGraph getObjectGraph() {
//
//        return null;
//    }
//
//    @Override
//    public void injectObjects(Object instance) {
//        Preconditions.checkNotNull(instance);
//        mObjectGraph.inject(instance);
//    }
//}
