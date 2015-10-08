package org.envirocar.app.injection;

import android.content.Context;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.ContextInternetAccessProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.util.Util;
import org.envirocar.remote.dao.CacheAnnouncementsDAO;
import org.envirocar.remote.dao.CacheCarDAO;
import org.envirocar.remote.dao.CacheFuelingDAO;
import org.envirocar.remote.dao.CacheTermsOfUseDAO;
import org.envirocar.remote.dao.CacheTrackDAO;
import org.envirocar.remote.dao.CacheUserDAO;
import org.envirocar.remote.dao.RemoteAnnouncementsDAO;
import org.envirocar.remote.dao.RemoteCarDAO;
import org.envirocar.remote.dao.RemoteFuelingDAO;
import org.envirocar.remote.dao.RemoteTermsOfUseDAO;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.dao.RemoteUserDAO;
import org.envirocar.remote.dao.RemoteUserStatisticsDAO;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for dao-specific dependencies which require a Application-
 * {@link Context} or to create. This includes specific Map- and
 * Bus-dependencies and
 *
 * @author dewall
 *
 */
@Module(
        injects = {
                DAOProvider.class,
                RemoteAnnouncementsDAO.class,
                CacheAnnouncementsDAO.class,
                RemoteFuelingDAO.class,
                CacheFuelingDAO.class,
                RemoteCarDAO.class,
                CacheCarDAO.class,
                RemoteTermsOfUseDAO.class,
                CacheTermsOfUseDAO.class,
                RemoteTrackDAO.class,
                CacheTrackDAO.class,
                RemoteUserDAO.class,
                CacheUserDAO.class,
                RemoteUserStatisticsDAO.class
        },
        addsTo = InjectionApplicationModule.class,
        library = true,
        complete = false
)
public class InjectionDAOModule {

    private final Context mContext;

    /**
     * Constructor.
     *
     * @param context   the context of the current scope.
     */
    public InjectionDAOModule(Context context){
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
