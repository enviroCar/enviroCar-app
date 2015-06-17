/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.dao;

import android.content.Context;
import android.os.AsyncTask;

import com.google.common.base.Preconditions;

import org.envirocar.app.InjectionModuleProvider;
import org.envirocar.app.Injector;
import org.envirocar.app.dao.cache.CacheAnnouncementsDAO;
import org.envirocar.app.dao.cache.CacheFuelingDAO;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.dao.cache.CacheTermsOfUseDAO;
import org.envirocar.app.dao.cache.CacheTrackDAO;
import org.envirocar.app.dao.cache.CacheUserDAO;
import org.envirocar.app.dao.exception.DAOException;
import org.envirocar.app.dao.remote.RemoteAnnouncementsDAO;
import org.envirocar.app.dao.remote.RemoteFuelingDAO;
import org.envirocar.app.dao.remote.RemoteSensorDAO;
import org.envirocar.app.dao.remote.RemoteTermsOfUseDAO;
import org.envirocar.app.dao.remote.RemoteTrackDAO;
import org.envirocar.app.dao.remote.RemoteUserDAO;
import org.envirocar.app.injection.DAOInjectionModule;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * the {@link DAOProvider} consists a set of methods
 * to access specific DAOs. It checks the internet connection
 * and decides whether it should use a Cache DAO or a Remote one.
 *
 * @author matthes rieke
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
        this.mObjectGraph = ((Injector) context).getObjectGraph().plus(getInjectionModules()
                .toArray());
        this.mObjectGraph.inject(this);
    }

    public static <T> void async(final AsyncExecutionWithCallback<T> callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                boolean fail = true;
                T result = null;
                Exception ex = null;
                try {
                    result = callback.execute();
                    fail = false;
                } catch (RuntimeException e) {
                    ex = e;
                } catch (DAOException e) {
                    ex = e;
                }
                callback.onResult(result, fail, ex);
                return null;
            }
        }.execute();
    }

    /**
     * @return the {@link SensorDAO}
     */
    public SensorDAO getSensorDAO() {
        CacheSensorDAO cacheSensorDao = mObjectGraph.get(CacheSensorDAO.class);
        if (this.mInternetAccessProvider.isConnected()) {
            // TODO use injection for this.
            RemoteSensorDAO remoteSensorDAO = new RemoteSensorDAO(cacheSensorDao);
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

    public AnnouncementsDAO getAnnouncementsDAO() {
        
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
        return Arrays.<Object>asList(new DAOInjectionModule(mAppContext));
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

    public static interface AsyncExecutionWithCallback<T> {

        public T execute() throws DAOException;

        public T onResult(T result, boolean fail, Exception exception);

    }

}
