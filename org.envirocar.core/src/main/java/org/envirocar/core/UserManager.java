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
package org.envirocar.core;

import org.envirocar.core.entity.User;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserManager {
    /**
     * Callback interface for the login process.
     */
    interface LoginCallback {
        /**
         * Called when the specific user has been successfully logged in.
         *
         * @param user the valid {@link User} instance that has been logged in.
         */
        void onSuccess(User user);

        /**
         * Called when the password is incorrect.
         *
         * @param password the incorrect password string.
         */
        void onPasswordIncorrect(String password);

        /**
         * Called when no connection could be established to the server.
         */
        void onUnableToCommunicateServer();
    }

    boolean isLoggedIn();

    void logOut();

    void logIn(String user, String token, LoginCallback callback);

    User getUser();

    void setUser(User user);
}
