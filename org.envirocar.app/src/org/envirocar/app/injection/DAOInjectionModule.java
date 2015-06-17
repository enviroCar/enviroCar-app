package org.envirocar.app.injection;

import android.content.Context;

import org.envirocar.app.InjectionApplicationModule;
import org.envirocar.app.application.ContextInternetAccessProvider;
import org.envirocar.app.dao.AnnouncementsDAO;
import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.FuelingDAO;
import org.envirocar.app.dao.InternetAccessProvider;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.dao.TermsOfUseDAO;
import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.dao.UserDAO;
import org.envirocar.app.util.Util;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for dao-specific dependencies which require a Application-
 * {@link android.content.Context} or to create. This includes specific Map- and
 * Bus-dependencies and
 *
 * @author dewall
 *
 */
@Module(
        injects = {
                DAOProvider.class,
                AnnouncementsDAO.class,
                FuelingDAO.class,
                SensorDAO.class,
                TermsOfUseDAO.class,
                TrackDAO.class,
                UserDAO.class
        },
        addsTo = InjectionApplicationModule.class,
        library = true,
        complete = false
)
public class DAOInjectionModule {

    private final Context mContext;

    /**
     * Constructor.
     *
     * @param context   the context of the current scope.
     */
    public DAOInjectionModule(Context context){
        this.mContext = context;
    }

    /**
     * Provides the InternetAccessProivder.
     *
     * @return the provider for internet access.
     */
    @Provides
    @Singleton
    public InternetAccessProvider provideInternetAccessProvider(){
        return new ContextInternetAccessProvider(mContext);
    }

    /**
     * Provides the CacheDirectoryProvider.
     *
     * @return the provider for cache access.
     */
    @Provides
    @Singleton
    public CacheDirectoryProvider provideCacheDirectoryProvider(){
        return new CacheDirectoryProvider() {
            @Override
            public File getBaseFolder() {
                return Util.resolveCacheFolder(mContext);
            }
        };
    }
}
