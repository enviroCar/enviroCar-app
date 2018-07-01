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


import org.envirocar.core.UserManager;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.dao.UserStatisticsDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RemoteUserStatisticsDAO extends BaseRemoteDAO<UserDAO, UserService> implements
        UserStatisticsDAO {

    /**
     * Constructor.
     *
     * @param cacheDao    the cache dao for users.
     * @param userService the user service.
     * @param userManager the user manager.
     */
    @Inject
    public RemoteUserStatisticsDAO(CacheUserDAO cacheDao, UserService userService, UserManager
            userManager) {
        super(cacheDao, userService, userManager);
    }

    @Override
    public UserStatistics getUserStatistics(User user) throws DataRetrievalFailureException {
        final UserService userService = EnviroCarService.getUserService();
        Call<UserStatistics> userStatistics = userService.getUserStatistics(user.getUsername());

        try {
            Response<UserStatistics> userStatisticsResponse = userStatistics.execute();

            if (userStatisticsResponse.isSuccess()) {
                return userStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = userStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, userStatisticsResponse
                        .errorBody().string());
                return null;
            }

        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<UserStatistics> getUserStatisticsObservable(String user) {
        return EnviroCarService
                .getUserService()
                .getUserStatisticsObservable(user);
    }
}
