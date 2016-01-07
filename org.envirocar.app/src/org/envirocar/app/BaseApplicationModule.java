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
package org.envirocar.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.app.handler.HandlerModule;
import org.envirocar.app.handler.TemporaryFileManager;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.services.NotificationHandler;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.services.TrackUploadService;
import org.envirocar.app.view.LoginActivity;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.carselection.CarSelectionAddCarFragment;
import org.envirocar.app.view.logbook.LogbookActivity;
import org.envirocar.app.view.logbook.LogbookAddFuelingFragment;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;
import org.envirocar.app.view.preferences.BluetoothDiscoveryIntervalPreference;
import org.envirocar.app.view.preferences.BluetoothPairingPreference;
import org.envirocar.app.view.preferences.SelectBluetoothPreference;
import org.envirocar.app.view.settings.SettingsActivity;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.app.view.trackdetails.TrackStatisticsActivity;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.obd.FeatureFlags;
import org.envirocar.remote.CacheModule;
import org.envirocar.remote.DAOProvider;
import org.envirocar.remote.RemoteModule;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.storage.DatabaseModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for application-specific dependencies which require a Application-
 * {@link Context} or to create. This includes specific Bus-dependencies.
 *
 * @author dewall
 */
@Module(
        includes = {
                RemoteModule.class,
                CacheModule.class,
                DatabaseModule.class,
                HandlerModule.class
        },
        injects = {
                BluetoothPairingPreference.class,
                SelectBluetoothPreference.class,
                TemporaryFileManager.class,
                SystemStartupService.class,
                NotificationHandler.class,
                BluetoothDiscoveryIntervalPreference.class,
                TrackDetailsActivity.class,
                CarSelectionActivity.class,
                OBDSelectionActivity.class,
                TrackStatisticsActivity.class,
                LoginActivity.class,
                SettingsActivity.class,
                TrackUploadService.class,
                LogbookActivity.class,
                LogbookAddFuelingFragment.class,
                CarSelectionAddCarFragment.class
        },
        staticInjections = {EnviroCarService.class},
        library = true,
        complete = false
)
public class BaseApplicationModule {
    private final Application mApplication;
    private final Context mAppContext;

    private Bus mBus;

    /**
     * Constructor.
     *
     * @param application the current application.
     */
    public BaseApplicationModule(Application application) {
        this.mApplication = application;
        this.mAppContext = application.getApplicationContext();
    }

    /**
     * Provides the Application of the App.
     *
     * @return the Application.
     */
    @Provides
    Application provideApplication() {
        return mApplication;
    }

    /**
     * Provides the Application Context.
     *
     * @return the context of the application.
     */
    @Provides
    @InjectApplicationScope
    Context provideApplicationContext() {
        return mAppContext;
    }

    /**
     * Provides the Application Injector.
     *
     * @return the Injector of the application.
     */
    @Provides
    @Singleton
    Injector provideApplicationInjector() {
        return (Injector) mApplication;
    }

    /**
     * Provides the event bus for the application.
     *
     * @return the application event bus.
     */
    @Provides
    Bus provideBus() {
        if (mBus == null)
            mBus = new Bus(ThreadEnforcer.ANY);
        return mBus;
    }

    /**
     * Provides the DAOProvider fot the application
     *
     * @return the DAOprovider of the application
     */
    @Provides
    @Singleton
    DAOProvider provideDAOProvider() {
        return new DAOProvider(mAppContext);
    }

    /**
     * Provides the FeatureFlags of the application
     *
     * @return the FeatureFlags of the application
     */
    @Provides
    @Singleton
    FeatureFlags provideFeatureFlagsManager() {
        return new FeatureFlags(mAppContext);
    }

    /**
     * Provides the TemporaryFileManager of the application
     *
     * @return the TemporaryFileManager of the application.
     */
    @Provides
    @Singleton
    TemporaryFileManager provideTemporaryFileManager() {
        return new TemporaryFileManager(mAppContext);
    }


    /**
     * Provides the CarManager of the application
     *
     * @return the CarManager of the application.
     */
    @Provides
    @Singleton
    NotificationHandler provideNotificationHandler() {
        return new NotificationHandler(mAppContext);
    }

    @Provides
    @Singleton
    TrackRecordingHandler provideTrackHandler() {
        return new TrackRecordingHandler(mAppContext);
    }

    @Provides
    @Singleton
    TrackDetailsProvider provideTrackDetailsProvider() { return new TrackDetailsProvider(mBus); }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(@InjectApplicationScope Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    RxSharedPreferences provideRxSharedPreferences(SharedPreferences prefs){
        return RxSharedPreferences.create(prefs);
    }
}
