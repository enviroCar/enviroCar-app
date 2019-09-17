/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.main;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.mapbox.mapboxsdk.Mapbox;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.annotation.AcraCore;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.handler.userstatistics.UserStatisticsProcessor;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.ACRASenderFactory;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.envirocar.remote.service.AnnouncementsService;
import org.envirocar.remote.service.CarService;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.FuelingService;
import org.envirocar.remote.service.TermsOfUseService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.service.UserService;

import javax.inject.Inject;


/**
 * @author dewall
 */
@AcraCore(buildConfigClass = BuildConfig.class, reportSenderFactoryClasses = ACRASenderFactory.class)
public class BaseApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(BaseApplication.class);

    BaseApplicationComponent baseApplicationComponent;
    protected BroadcastReceiver mScreenReceiver;

    private String CHANNEL_ID = "channel1";

    @Inject
    protected UserService userService;
    @Inject
    protected CarService carService;
    @Inject
    protected TrackService trackService;
    @Inject
    protected TermsOfUseService termsOfUseService;
    @Inject
    protected FuelingService fuelingService;
    @Inject
    protected AnnouncementsService announcementsService;
    @InjectApplicationScope
    @Inject
    protected Context context;
    @Inject
    protected UserStatisticsProcessor statisticsProcessor;
    @Inject
    protected LocationHandler locationHandler;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener
            = (sharedPreferences, key) -> {
        if (PreferenceConstants.ENABLE_DEBUG_LOGGING.equals(key)) {
            Logger.initialize(Util.getVersionString(BaseApplication.this),
                    sharedPreferences.getBoolean(PreferenceConstants.ENABLE_DEBUG_LOGGING, false));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Mapbox.getInstance(this, "");

        baseApplicationComponent =
                DaggerBaseApplicationComponent
                        .builder()
                        .baseApplicationModule(new BaseApplicationModule(this))
                        .build();
        baseApplicationComponent.inject(this);

        EnviroCarService.setCarService(carService);
        EnviroCarService.setAnnouncementsService(announcementsService);
        EnviroCarService.setFuelingService(fuelingService);
        EnviroCarService.setTermsOfUseService(termsOfUseService);
        EnviroCarService.setTrackService(trackService);
        EnviroCarService.setUserService(userService);
        NotificationHandler.context = context;

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);

        // Initialize ACRA
        ACRA.init(this);

        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    // do whatever you need to do here
                    LOGGER.info("SCREEN IS OFF");
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    // and do whatever you need to do here
                    LOGGER.info("SCREEN IS ON");
                }
            }
        };



        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean obfus = prefs.getBoolean(PreferenceConstants.OBFUSCATE_POSITION, false);

        LOGGER.info("Obfuscation enabled? " + obfus);

        Logger.initialize(Util.getVersionString(this),
                prefs.getBoolean(PreferenceConstants.ENABLE_DEBUG_LOGGING, false));


    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mScreenReceiver != null)
            unregisterReceiver(mScreenReceiver);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOGGER.info("onLowMemory called");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOGGER.info("onTrimMemory called");
        LOGGER.info("maxMemory: " + Runtime.getRuntime().maxMemory());
        LOGGER.info("totalMemory: " + Runtime.getRuntime().totalMemory());
        LOGGER.info("freeMemory: " + Runtime.getRuntime().freeMemory());
    }

    public BaseApplicationComponent getBaseApplicationComponent() {
        return baseApplicationComponent;
    }

    public static BaseApplication get(Context context) {
        return (BaseApplication) context.getApplicationContext();
    }


}
