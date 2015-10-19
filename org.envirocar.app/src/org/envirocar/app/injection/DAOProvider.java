package org.envirocar.app.injection;

import android.content.Context;

import com.google.common.base.Preconditions;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.dao.AnnouncementDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.dao.UserStatisticsDAO;
import org.envirocar.core.injection.InjectionModuleProvider;
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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DAOProvider implements Injector, InjectionModuleProvider {

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
        this.mObjectGraph = ((Injector) context).getObjectGraph()
                .plus(getInjectionModules().toArray());
        this.mObjectGraph.inject(this);
    }

    /**
     * @return the {@link CarDAO}
     */
    public CarDAO getSensorDAO() {
        CacheCarDAO cacheSensorDao = mObjectGraph.get(CacheCarDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            // TODO use injection for this.
            RemoteCarDAO remoteSensorDAO = new RemoteCarDAO(cacheSensorDao);
            injectObjects(remoteSensorDAO);
            return remoteSensorDAO;
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
        return new CacheTrackDAO();
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
        CacheFuelingDAO cacheFuelingDAO = mObjectGraph.get(CacheFuelingDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            RemoteFuelingDAO remoteFuelingDAO = new RemoteFuelingDAO(cacheFuelingDAO);
            injectObjects(remoteFuelingDAO);
            return remoteFuelingDAO;
        }
        return cacheFuelingDAO;
    }

    /**
     * @return the {@link TermsOfUseDAO}
     */
    public TermsOfUseDAO getTermsOfUseDAO() {
        CacheTermsOfUseDAO cacheTermsOfUseDAO = mObjectGraph.get(CacheTermsOfUseDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            RemoteTermsOfUseDAO remoteTermsOfUseDAO = new RemoteTermsOfUseDAO(cacheTermsOfUseDAO);
            injectObjects(remoteTermsOfUseDAO);
            return remoteTermsOfUseDAO;
        }
        return cacheTermsOfUseDAO;
    }

    public AnnouncementDAO getAnnouncementsDAO() {

        CacheAnnouncementsDAO cacheAnnouncementsDAO = mObjectGraph.get(CacheAnnouncementsDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            RemoteAnnouncementsDAO remoteAnnouncementsDAO = new RemoteAnnouncementsDAO
                    (cacheAnnouncementsDAO);
            injectObjects(remoteAnnouncementsDAO);
            return remoteAnnouncementsDAO;
        }
        return cacheAnnouncementsDAO;
    }

    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new InjectionDAOModule(mAppContext));
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    @Override
    public void injectObjects(Object instance) {
        Preconditions.checkNotNull(instance, "The instance of the object to get injected cannot " +
                "be null");
        mObjectGraph.inject(instance);
    }
}
