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
package org.envirocar.remote.dao;

import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.ResourceConflictException;

import rx.Observable;

/**
 * @author dewall
 */
public class CacheUserDAO implements UserDAO {

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