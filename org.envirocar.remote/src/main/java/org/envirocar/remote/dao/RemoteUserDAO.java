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

import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.requests.CreateUserRequest;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class RemoteUserDAO extends BaseRemoteDAO<UserDAO, UserService> implements UserDAO {
    private static final Logger LOG = Logger.getLogger(RemoteUserDAO.class);

    @Inject
    public RemoteUserDAO(CacheUserDAO cacheDao, UserService userService) {
        super(cacheDao, userService);
    }

    @Override
    public User getUser(String id) throws DataRetrievalFailureException, UnauthorizedException, NotConnectedException, ResourceConflictException {
        // Get the remoteService for the getUserStatistic endpoints and initiates a call.
        Call<User> userCall = remoteService.getUser(id);
        try {
            return executeCall(userCall).body();
        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<User> getUserObservable(String id) {
        // Get the remoteService for the getUserStatistic endpoints and returns an getUserStatistic observable.
        return remoteService.getUserObservable(id);
    }

    @Override
    public void createUser(User newUser) throws DataUpdateFailureException,
            ResourceConflictException {
        try {
            // Get the remoteService for the getUserStatistic endpoints and initiate a call.
            Call<ResponseBody> userCall = remoteService.createUser( // Workaround
                    new CreateUserRequest(newUser.getUsername(), newUser.getMail(), newUser.getToken(), true, true));
            executeCall(userCall);
        } catch (IOException e) {
            throw new DataUpdateFailureException(e);
        } catch (NotConnectedException e) {
            throw new DataUpdateFailureException(e);
        } catch (UnauthorizedException e) {
            throw new DataUpdateFailureException(e);
        }
    }

    @Override
    public void updateUser(User user) throws DataUpdateFailureException {
        // Workaround: The server only requires mail and TOU version to update the
        // terms of use.  The serialization, however, serializes everything. If the
        // request body contains the username as well as the token, then it throws an 405.
        User update = user.carbonCopy();
        update.setUsername(null);
        update.setToken(null);

        // Get the remoteService for the getUserStatistic endpoints and initiate a call.
        UserService userService = EnviroCarService.getUserService();
        Call<ResponseBody> userCall = userService.updateUser(user.getUsername(), update);

        try {
            // execute the call
            Response<ResponseBody> userResponse = userCall.execute();

            // If the execution was not a success, then throw an error.
            if (!userResponse.isSuccessful()) {
                LOG.severe("updateUser(): Error while updating remote getUserStatistic");
                EnvirocarServiceUtils.assertStatusCode(userResponse.code(), userResponse.message());
            }
        } catch (IOException e) {
            throw new DataUpdateFailureException(e);
        } catch (Exception e) {
            throw new DataUpdateFailureException(e);
        }
    }

}
