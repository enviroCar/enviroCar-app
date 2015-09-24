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
package org.envirocar.app.model.dao.remote;

import com.squareup.okhttp.ResponseBody;

import org.apache.http.HttpStatus;
import org.envirocar.app.model.User;
import org.envirocar.app.model.dao.UserDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.exception.UserRetrievalException;
import org.envirocar.app.model.dao.exception.UserUpdateException;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.UserService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;

import java.io.IOException;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * @author dewall
 */
public class RemoteUserDAO extends BaseRemoteDAO implements UserDAO, AuthenticatedDAO {

    @Override
    public User getUser(String id) throws UserRetrievalException, UnauthorizedException {
        // Get the service for the user endpoints and initiates a call.
        UserService userService = EnviroCarService.getUserService();
        Call<User> userCall = userService.getUser(id);

        try {
            // execute the call
            Response<User> userResponse = userCall.execute();
            // If the execution was successful, then return the user instance.
            if (userResponse.isSuccess()) {
                return userResponse.body();
            }
            // if not, then get the error code and throw a corresponding exception.
            else {
                String message = userResponse.message();
                if (userResponse.raw().code() == HttpStatus.SC_UNAUTHORIZED
                        || userResponse.raw().code() == HttpStatus.SC_FORBIDDEN) {
                    throw new UnauthorizedException(userResponse.errorBody().string());
                } else {
                    throw new UserRetrievalException(userResponse.errorBody().string());
                }
            }
        } catch (IOException e) {
            throw new UserRetrievalException(e);
        }
    }

    @Override
    public Observable<User> getUserObservable(String id) {
        // Get the service for the user endpoints and returns an user observable.
        UserService userService = EnviroCarService.getUserService();
        return userService.getUserObservable(id);
    }

    @Override
    public void createUser(User newUser) throws UserUpdateException, ResourceConflictException {
        // Get the service for the user endpoints and initiate a call.
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
            throw new UserUpdateException(e);
        } catch (NotConnectedException e) {
            throw new UserUpdateException(e);
        } catch (UnauthorizedException e) {
            throw new UserUpdateException(e);
        }
    }

    @Override
    public void updateUser(User user) throws UserUpdateException, UnauthorizedException {
        // Get the service for the user endpoints and initiate a call.
        UserService userService = EnviroCarService.getUserService();
        Call<ResponseBody> userCall = userService.updateUser(user);

        try {
            // execute the call
            Response<ResponseBody> userResponse = userCall.execute();

            // If the execution was not a success, then throw an error.
            if (!userResponse.isSuccess()) {
                if (userResponse.raw().code() == HttpStatus.SC_UNAUTHORIZED
                        || userResponse.raw().code() == HttpStatus.SC_FORBIDDEN) {
                    throw new UnauthorizedException(userResponse.errorBody().string());
                } else {
                    throw new UserUpdateException(userResponse.errorBody().string());
                }
            }
        } catch (IOException e) {
            throw new UserUpdateException(e);
        }

    }
}
