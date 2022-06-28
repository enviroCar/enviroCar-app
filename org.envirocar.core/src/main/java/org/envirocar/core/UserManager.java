/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.core;

import org.envirocar.core.entity.User;
import org.envirocar.core.exception.NotConnectedException;

import io.reactivex.Completable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserManager {

    /**
     * Determines whether the getUserStatistic is logged in. A getUserStatistic is logged in when
     * the application has a getUserStatistic as a variable.
     *
     * @return
     */
    boolean isLoggedIn();

    /**
     * Get the getUserStatistic
     *
     * @return getUserStatistic
     */
    User getUser();

    /**
     * Get the getUserStatistic, but the most recent version from the remote DAO
     *
     * @return getUserStatistic
     */
    public User retrieveUpdatedUser(User user) throws NotConnectedException;

    /**
     * Sets the getUserStatistic
     *
     * @param user
     */
    void setUser(User user);

    /**
     * Handles the login as a completable
     *
     * @param user  username
     * @param token getUserStatistic token
     * @return
     */
    Completable logIn(String user, String token);

    Completable logIn(String user, String token, boolean withEvent);

    /**
     * Handles the logout procedure
     *
     * @return a completable handling the logout
     */
    Completable logOut();

    Completable logOut(Boolean withEvent);
}
