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
package org.envirocar.app.model.dao;

import android.database.Observable;

import org.envirocar.app.model.User;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.exception.UserRetrievalException;
import org.envirocar.app.model.dao.exception.UserUpdateException;

/**
 * DAO interface for user related action.
 *
 * @author dewall
 */
public interface UserDAO {

    /**
     * Gets the user instance for a given username. Becuase this methods is synchronous, it has
     * to be executed on a separate thread, whcih is not the main thread.
     *
     * @param id the name of the user.
     * @return the instance of the user.
     * @throws UserRetrievalException thrown when the user can't be received.
     * @throws UnauthorizedException  thrown when the user is not authorized to do that.
     */
    User getUser(String id) throws UserRetrievalException, UnauthorizedException;

    /**
     * Gets an user observable for a given username.
     *
     * @param id the name of the user.
     * @return the instance of the user.
     */
    rx.Observable<User> getUserObservable(String id);

    /**
     * Creates a new user on the server.
     *
     * @param newUser the new user instance.
     * @throws UserUpdateException
     * @throws ResourceConflictException
     */
    void createUser(User newUser) throws UserUpdateException, ResourceConflictException;

    /**
     * Updates the information of a user at the server.
     *
     * @param user the instance of the user.
     * @throws UserUpdateException   thrown when an error occured while updating the user.
     * @throws UnauthorizedException thrown when this user is not authorized to do that.
     */
    void updateUser(User user) throws UserUpdateException, UnauthorizedException;
}
