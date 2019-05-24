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
package org.envirocar.app.main;

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
import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.app.services.OBDServiceModule;
import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.remote.RemoteModule;
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
                DatabaseModule.class,
                HandlerModule.class,
                OBDServiceModule.class
        }

)
public class BaseApplicationModule {
    private static final Logger LOGGER = Logger.getLogger(BaseApplicationModule.class);

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
     * Provides the TemporaryFileManager of the application
     *
     * @return the TemporaryFileManager of the application.
     */
    @Provides
    @Singleton
    TemporaryFileManager provideTemporaryFileManager() {
        return new TemporaryFileManager(mAppContext);
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

    /**
     * Provides the CacheDirectoryProvider.
     *
     * @return the provider for cache access.
     */
    @Provides
    @Singleton
    public CacheDirectoryProvider provideCacheDirectoryProvider(
            @InjectApplicationScope Context context) {
        return () -> Util.resolveCacheFolder(context);
    }
}
