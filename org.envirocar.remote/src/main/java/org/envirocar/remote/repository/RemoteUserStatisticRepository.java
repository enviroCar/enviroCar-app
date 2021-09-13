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
package org.envirocar.remote.repository;

import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.repository.UserStatisticRepository;
import org.envirocar.remote.service.UserService;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
@Singleton
public class RemoteUserStatisticRepository extends RemoteRepository<UserService> implements UserStatisticRepository {

    private final UserService userService;

    /**
     * Constructor.
     *
     * @param userService
     */
    @Inject
    public RemoteUserStatisticRepository(UserService userService, InternetAccessProvider accessProvider) {
        super(userService, accessProvider);
        this.userService = userService;
    }

    @Override
    public Observable<UserStatistic> getUserStatistic(String username) {
        Call<UserStatistic> call = userService.getUserStatistic(username);
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }

}
