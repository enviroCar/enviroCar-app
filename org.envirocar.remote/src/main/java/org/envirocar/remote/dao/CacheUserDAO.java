/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.remote.dao;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.dao.AbstractCacheDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.ResourceConflictException;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

/**
 * @author dewall
 */
@Singleton
public class CacheUserDAO extends AbstractCacheDAO implements UserDAO {

    @Inject
    public CacheUserDAO(CacheDirectoryProvider provider) {
        super(provider);
    }

    @Override
    public void updateUser(User user) throws DataUpdateFailureException {
        throw new DataUpdateFailureException("Not supported by Cache");
    }

    @Override
    public User getUser(String id) throws DataRetrievalFailureException {
        throw new DataRetrievalFailureException("Not supported by Cache");
    }

    @Override
    public Observable<User> getUserObservable(String id) {
        return Observable.error(new DataUpdateFailureException("Not supported by Cache"));
    }

    @Override
    public void createUser(User user) throws DataUpdateFailureException, ResourceConflictException {
        throw new DataUpdateFailureException("Not supported by Cache");
    }

}
