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
package org.envirocar.remote.util;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;

import org.envirocar.core.UserManager;
import org.envirocar.core.entity.User;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class AuthenticationInterceptor implements Interceptor {

    protected final UserManager mUserManager;

    /**
     * Constructor.
     *
     * @param userManager the user manager.
     */
    @Inject
    public AuthenticationInterceptor(UserManager userManager) {
        this.mUserManager = userManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (mUserManager.isLoggedIn()) {
            User user = mUserManager.getUser();
            return chain.proceed(
                    chain.request()
                            .newBuilder()
                            .addHeader("X-User", user.getUsername())
                            .addHeader("X-Token", user.getToken())
                            .build());
        } else {
            return chain.proceed(chain.request());
        }
    }
}
