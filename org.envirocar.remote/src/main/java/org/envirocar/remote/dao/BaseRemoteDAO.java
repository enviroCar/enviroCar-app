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
package org.envirocar.remote.dao;

import org.envirocar.core.UserManager;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BaseRemoteDAO<C, S> {

    protected final C cacheDao;
    protected final S remoteService;
    protected final UserManager userManager;

    /**
     * Constructor.
     *
     * @param cacheDao      the caching dao.
     * @param remoteService the remote service of this dao.
     */
    public BaseRemoteDAO(C cacheDao, S remoteService) {
        this(cacheDao, remoteService, null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao      the cache dao
     * @param remoteService the remote service of this dao
     * @param userManager   the user manager
     */
    public BaseRemoteDAO(C cacheDao, S remoteService, UserManager userManager) {
        this.userManager = userManager;
        this.cacheDao = cacheDao;
        this.remoteService = remoteService;
    }

    protected <T> Response<T> executeCall(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        Response<T> response = call.execute();

        // assert the responsecode if it was not an success.
        if (!response.isSuccessful()) {
            EnvirocarServiceUtils.assertStatusCode(response.code(),
                    response.message());
        }

        return response;
    }

}
