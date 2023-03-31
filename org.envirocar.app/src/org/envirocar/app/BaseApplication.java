/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.justai.aimybox.Aimybox;
import com.justai.aimybox.components.AimyboxAssistantViewModel;
import com.justai.aimybox.components.AimyboxProvider;
import com.mapbox.mapboxsdk.Mapbox;
import com.squareup.otto.Bus;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.annotation.AcraCore;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.userstatistics.UserStatisticsProcessor;
import org.envirocar.app.notifications.AutomaticUploadNotificationHandler;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.rxutils.RxBroadcastReceiver;
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
import org.envirocar.voicecommand.BaseAimybox;
import org.envirocar.voicecommand.handler.MetadataHandler;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;



/**
 * @author dewall
 */

@AcraCore(buildConfigClass = BuildConfig.class, reportSenderFactoryClasses = ACRASenderFactory.class)
public class BaseApplication extends Application implements AimyboxProvider {
    private static Logger LOG = Logger.getLogger(BaseApplication.class);

    BaseApplicationComponent baseApplicationComponent;
    protected BroadcastReceiver mScreenReceiver;

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
    @Inject
    protected AutomaticUploadNotificationHandler automaticUploadHandler;
    @Inject
    protected MetadataHandler metadataHandler;
    @Inject
    protected Bus mBus;

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        // hack
        Logger.addFileHandlerLocation(getFilesDir().getAbsolutePath());

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

        // Initialize ACRA
        ACRA.init(this);

        // debug logging setting listener
        this.disposables.add(
                ApplicationSettings.getDebugLoggingObservable(this)
                        .doOnNext(this::setDebugLogging)
                        .doOnError(LOG::error)
                        .subscribe());

        // obfuscation setting changed listener
        this.disposables.add(
                ApplicationSettings.getObfuscationObservable(this)
                        .doOnNext(bool -> LOG.info("Obfuscation enabled: %s", bool.toString()))
                        .doOnError(LOG::error)
                        .subscribe());

        // register Intentfilter for logging screen changes
        IntentFilter screenIntentFilter = new IntentFilter();
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        this.disposables.add(
                RxBroadcastReceiver.create(this, screenIntentFilter)
                        .doOnNext(intent -> {
                            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                                // do whatever you need to do here
                                LOG.info("SCREEN IS OFF");
                            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                                // and do whatever you need to do here
                                LOG.info("SCREEN IS ON");
                            }
                        })
                        .doOnError(LOG::error)
                        .subscribe());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mScreenReceiver != null) {
            unregisterReceiver(mScreenReceiver);
        }

        if (disposables != null) {
            disposables.clear();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOG.info("onLowMemory called");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOG.info("onTrimMemory called");
        LOG.info("maxMemory: " + Runtime.getRuntime().maxMemory());
        LOG.info("totalMemory: " + Runtime.getRuntime().totalMemory());
        LOG.info("freeMemory: " + Runtime.getRuntime().freeMemory());
    }

    private void setDebugLogging(Boolean isDebugLoggingEnabled) {
        LOG.info("Received change in debug log level. Is enabled=", isDebugLoggingEnabled.toString());
        Logger.initialize(Util.getVersionString(BaseApplication.this), isDebugLoggingEnabled);
    }

    public BaseApplicationComponent getBaseApplicationComponent() {
        return baseApplicationComponent;
    }

    public static BaseApplication get(Context context) {
        return (BaseApplication) context.getApplicationContext();
    }

    @NonNull
    @Override
    public Aimybox getAimybox() {
        BaseAimybox.Companion.setCurrentAimybox(new BaseAimybox(this, mBus, metadataHandler).getAimybox());
        return BaseAimybox.Companion.getCurrentAimybox();
    }

    @NonNull
    @Override
    public AimyboxAssistantViewModel.Factory getViewModelFactory() {
        if(BaseAimybox.Companion.getCurrentAimybox() == null){
            return AimyboxAssistantViewModel.Factory.Companion.getInstance(getAimybox());
        }
        return AimyboxAssistantViewModel.Factory.Companion.getInstance(BaseAimybox.Companion.getCurrentAimybox());
    }
}