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

import com.squareup.okhttp.ResponseBody;

import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class RemoteUserDAO extends BaseRemoteDAO<UserDAO, UserService> implements UserDAO {
    private static final Logger LOG = Logger.getLogger(RemoteUserDAO.class);

    @Inject
    public RemoteUserDAO(CacheUserDAO cacheDao, UserService userService){
        super(cacheDao, userService);
    }

    @Override
    public User getUser(String id) throws DataRetrievalFailureException, UnauthorizedException, NotConnectedException {
        // Get the remoteService for the user endpoints and initiates a call.
        UserService userService = EnviroCarService.getUserService();
        Call<User> userCall = userService.getUser(id);

        try {
            // execute the call
            Response<User> userResponse = userCall.execute();
            // If the execution was successful, then return the user instance. if not, then get
            // the error code and throw a corresponding exception.
            if (!userResponse.isSuccess()) {
                LOG.severe("Error while retrieving remote user of id = " + id);
                EnvirocarServiceUtils.assertStatusCode(userResponse.code(), userResponse.message());
            }

            return userResponse.body();
        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        } catch (ResourceConflictException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<User> getUserObservable(String id) {
        // Get the remoteService for the user endpoints and returns an user observable.
        UserService userService = EnviroCarService.getUserService();
        return userService.getUserObservable(id);
    }

    @Override
    public void createUser(User newUser) throws DataUpdateFailureException,
            ResourceConflictException {
        // Get the remoteService for the user endpoints and initiate a call.
        UserService userService = EnviroCarService.getUserService();
        Call<ResponseBody> userCall = userService.createUser(newUser);

        Response<ResponseBody> userResponse = null;
        try {
            // execute the call
            userResponse = userCall.execute();
            // If the execution was successful, then throw an exception.
            if (!userResponse.isSuccess()) {
                int responseCode = userResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, userResponse
                        .errorBody().string());
            }
        } catch (IOException e) {
            throw new DataUpdateFailureException(e);
        } catch (NotConnectedException e) {
            throw new DataUpdateFailureException(e);
        } catch (UnauthorizedException e) {
            throw new DataUpdateFailureException(e);
        }
    }

    @Override
    public void updateUser(User user) throws DataUpdateFailureException, UnauthorizedException {
        // Workaround: The server only requires mail and TOU version to update the
        // terms of use.  The serialization, however, serializes everything. If the
        // request body contains the username as well as the token, then it throws an 405.
        User update = user.carbonCopy();
        update.setUsername(null);
        update.setToken(null);

        // Get the remoteService for the user endpoints and initiate a call.
        UserService userService = EnviroCarService.getUserService();
        Call<ResponseBody> userCall = userService.updateUser(user.getUsername(), update);

        try {
            // execute the call
            Response<ResponseBody> userResponse = userCall.execute();

            // If the execution was not a success, then throw an error.
            if (!userResponse.isSuccess()) {
                LOG.severe("updateUser(): Error while updating remote user");
                EnvirocarServiceUtils.assertStatusCode(userResponse.code(), userResponse.message());
            }
        } catch (IOException e) {
            throw new DataUpdateFailureException(e);
        } catch (Exception e) {
            throw new DataUpdateFailureException(e);
        }
    }

}
