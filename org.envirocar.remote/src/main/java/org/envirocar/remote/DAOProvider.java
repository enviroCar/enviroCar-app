package org.envirocar.remote;

import android.content.Context;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.dao.AnnouncementDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.dao.UserStatisticsDAO;
import org.envirocar.core.injection.Injector;
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

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DAOProvider {

    // No injection here.
    protected Context mAppContext;

    // Injected variables
    @Inject
    protected InternetAccessProvider mInternetAccessProvider;
    @Inject
    protected CacheDirectoryProvider mCacheDirectoryProvider;

    // Graph for Dependency Injection.
    private ObjectGraph mObjectGraph;

    /**
     * Constructor.
     *
     * @param context
     */
    public DAOProvider(final Context context) {
        this.mAppContext = context;

        // Extend the object graph with the injection modules for DAOs
        this.mObjectGraph = ((Injector) context).getObjectGraph();
        this.mObjectGraph.inject(this);
    }

    /**
     * @return the {@link CarDAO}
     */
    public CarDAO getSensorDAO() {
        CacheCarDAO cacheSensorDao = mObjectGraph.get(CacheCarDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteCarDAO.class);
        }
        return cacheSensorDao;
    }

    /**
     * @return the {@link TrackDAO}
     */
    public TrackDAO getTrackDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteTrackDAO.class);
        }
        return mObjectGraph.get(CacheTrackDAO.class);
    }

    /**
     * @return the {@link UserDAO}
     */
    public UserDAO getUserDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteUserDAO.class);
        }
        return mObjectGraph.get(CacheUserDAO.class);
    }

    public UserStatisticsDAO getUserStatisticsDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteUserStatisticsDAO.class);
        }
        return mObjectGraph.get(RemoteUserStatisticsDAO.class);
    }

    /**
     * @return the {@link FuelingDAO}
     */
    public FuelingDAO getFuelingDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteFuelingDAO.class);
        }
        return mObjectGraph.get(CacheFuelingDAO.class);
    }

    /**
     * @return the {@link TermsOfUseDAO}
     */
    public TermsOfUseDAO getTermsOfUseDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteTermsOfUseDAO.class);
        }
        return mObjectGraph.get(CacheTermsOfUseDAO.class);
    }

    public AnnouncementDAO getAnnouncementsDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return mObjectGraph.get(RemoteAnnouncementsDAO.class);
        }
        return mObjectGraph.get(CacheAnnouncementsDAO.class);
    }
}

