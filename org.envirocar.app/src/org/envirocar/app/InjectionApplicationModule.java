package org.envirocar.app;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;

import org.envirocar.app.Injector;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for application-specific dependencies which require a Application-
 * {@link android.content.Context} or to create. This includes specific Map- and
 * Bus-dependencies and
 *
 * @author Arne de Wall <a.dewall@esri.de>
 */
@Module(
        library = true
// complete = false,
// injects = { MainActivity.class, MapViewFragment.class }
)
public class InjectionApplicationModule {

    private final Application mApplication;
    private final Context mAppContext;

    public InjectionApplicationModule(Application application) {
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
    @Singleton
    Bus provideBus() {
        return new Bus();
    }

}