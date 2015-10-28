package org.envirocar.remote;

import android.content.Context;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.injection.InjectApplicationScope;
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
import org.envirocar.remote.service.EnviroCarService;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {
                DAOProvider.class,
                CacheUserDAO.class,
                CacheCarDAO.class,
                CacheFuelingDAO.class,
                CacheTermsOfUseDAO.class,
                CacheTrackDAO.class,
                CacheAnnouncementsDAO.class,
                RemoteAnnouncementsDAO.class,
                RemoteFuelingDAO.class,
                RemoteCarDAO.class,
                RemoteTermsOfUseDAO.class,
                RemoteTrackDAO.class,
                RemoteUserDAO.class,
                RemoteUserStatisticsDAO.class,
                DAOProvider.class
        },
        staticInjections = EnviroCarService.class
)
public class CacheModule {

    /**
     * Provides the CacheDirectoryProvider.
     *
     * @return the provider for cache access.
     */
    @Provides
    @Singleton
    public CacheDirectoryProvider provideCacheDirectoryProvider(
            @InjectApplicationScope Context context) {
        return new CacheDirectoryProvider() {
            @Override
            public File getBaseFolder() {
                return Util.resolveCacheFolder(context);
            }
        };
    }

}
