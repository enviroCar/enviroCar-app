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
package org.envirocar.app.handler;

import android.content.Context;

import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.dao.AnnouncementDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.dao.UserStatisticsDAO;
import org.envirocar.remote.dao.CacheCarDAO;

import javax.inject.Inject;

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

    BaseApplicationComponent baseApplicationComponent;

    /**
     * Constructor.
     *
     * @param context
     */
    public DAOProvider(final Context context) {
        this.mAppContext = context;

        // Extend the object graph with the injection modules for DAOs
        baseApplicationComponent = BaseApplication.get(context).getBaseApplicationComponent();
        baseApplicationComponent.inject(this);

    }

    /**
     * @return the {@link CarDAO}
     */
    public CarDAO getSensorDAO() {
        CacheCarDAO cacheSensorDao = baseApplicationComponent.getCacheCarDAO();
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteCarDAO();
        }
        return cacheSensorDao;
    }

    /**
     * @return the {@link TrackDAO}
     */
    public TrackDAO getTrackDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteTrackDAO();
        }
        return baseApplicationComponent.getCacheTrackDAO();
    }

    /**
     * @return the {@link UserDAO}
     */
    public UserDAO getUserDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteUserDAO();
        }
        return baseApplicationComponent.getCacheUserDAO();
    }

    public UserStatisticsDAO getUserStatisticsDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteUserStatisticsDAO();
        }
        return baseApplicationComponent.getRemoteUserStatisticsDAO();
    }

    /**
     * @return the {@link FuelingDAO}
     */
    public FuelingDAO getFuelingDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteFuelingDAO();
        }
        return baseApplicationComponent.getCacheFuelingDAO();
    }

    /**
     * @return the {@link TermsOfUseDAO}
     */
    public TermsOfUseDAO getTermsOfUseDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteTermsOfUseDAO();
        }
        return baseApplicationComponent.getCacheTermsOfUseDAO();
    }

    public AnnouncementDAO getAnnouncementsDAO() {
        if (this.mInternetAccessProvider.isConnected()) {
            return baseApplicationComponent.getRemoteAnnouncementsDAO();
        }
        return baseApplicationComponent.getCacheAnnouncementsDAO();
    }
}

