package org.envirocar.app.injection.module;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.envirocar.app.FeatureFlags;
import org.envirocar.app.LocationHandler;
import org.envirocar.app.NotificationHandler;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.activity.LogbookFragment;
import org.envirocar.app.activity.LoginFragment;
import org.envirocar.app.activity.RegisterFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.application.Collector;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.fragments.NewDashboardFragment;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;
import org.envirocar.app.view.tracklist.NewListFragment;
import org.envirocar.app.fragments.SettingsFragment;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.storage.LazyLoadingStrategy;
import org.envirocar.app.storage.LazyLoadingStrategyImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.view.preferences.BluetoothDiscoveryIntervalPreference;
import org.envirocar.app.view.preferences.BluetoothPairingPreference;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.view.preferences.SelectBluetoothPreference;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for application-specific dependencies which require a Application-
 * {@link android.content.Context} or to create. This includes specific Bus-dependencies.
 *
 *
 * @author dewall
 */
@Module(
        injects = {
                TermsOfUseManager.class,
                CarPreferenceHandler.class,
                ListTracksFragment.class,
                LogbookFragment.class,
                LoginFragment.class,
                RegisterFragment.class,
                SettingsActivity.class,
                CarSelectionPreference.class,
                BluetoothPairingPreference.class,
                BluetoothHandler.class,
                SettingsFragment.class,
                SelectBluetoothPreference.class,
                TemporaryFileManager.class,
                SystemStartupService.class,
                NotificationHandler.class,
                CommandListener.class,
                DbAdapterImpl.class,
                LocationHandler.class,
                OBDConnectionService.class,
                BluetoothDiscoveryIntervalPreference.class,
                Collector.class,
                LazyLoadingStrategyImpl.class,
                TrackHandler.class,
                UserManager.class,
                NewDashboardFragment.class,
                NewListFragment.class,
                TrackDetailsActivity.class,
                CarSelectionActivity.class,
                OBDSelectionActivity.class
        },
        staticInjections = { Track.class },
        library = true,
        complete = false
)
public class InjectionApplicationModule {
    private static final Logger LOGGER = Logger.getLogger(InjectionApplicationModule.class);

    private final Application mApplication;
    private final Context mAppContext;

    /**
     * Constructor.
     *
     * @param application   the current application.
     */
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
    @Singleton
    Bus provideBus() {
        return new Bus(ThreadEnforcer.ANY);
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
     * Provides the UserManager of the application
     *
     * @return the UserManager of the application
     */
    @Provides
    @Singleton
    UserManager provideUserManager() {
        return new UserManager(mAppContext);
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
     * Provides the TemporaryFileManager of the application
     *
     * @return the TemporaryFileManager of the application.
     */
    @Provides
    @Singleton
    DbAdapter provideDBAdapter() {

        DbAdapter adapter = null;
        try {
            adapter = new DbAdapterImpl(mAppContext);
        } catch (InstantiationException e) {
            LOGGER.warn("Could not initalize the database layer. The app will probably work " +
                    "unstable.");
            LOGGER.warn(e.getMessage(), e);
        }

        return adapter;
    }

    /**
     * Provides the TermsOfUseManager of the application
     *
     * @return the TermsOfUseManager of the application.
     */
    @Provides
    @Singleton
    TermsOfUseManager provideTermsOfUseManager() {
        return new TermsOfUseManager(mAppContext);
    }

    /**
     * Provides the CarManager of the application
     *
     * @return the CarManager of the application.
     */
    @Provides
    @Singleton
    CarPreferenceHandler provideCarManager() {
        return new CarPreferenceHandler(mAppContext);
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
    BluetoothHandler provideBluetoothHandler(){
        return new BluetoothHandler(mAppContext);
    }

    /**
     * Provides the LocationHandler of the application.
     *
     * @return the LocationHandler of the application.
     */
    @Provides
    @Singleton
    LocationHandler provideLocationHandler(){
        return new LocationHandler(mAppContext);
    }

    /**
     * Provides the LazyLoadingStrategy of the application.
     *
     * @return the LazyLoadingStrategy of the application.
     */
    @Provides
    @Singleton
    LazyLoadingStrategy provideLazyLoadingStrategy() {
        return new LazyLoadingStrategyImpl(mAppContext);
    }

    @Provides
    @Singleton
    TrackHandler provideTrackHandler(){
        return new TrackHandler(mAppContext);
    }
}