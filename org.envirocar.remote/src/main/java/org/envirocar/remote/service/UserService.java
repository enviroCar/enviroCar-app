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
package org.envirocar.remote.service;


import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.remote.requests.CreateUserRequest;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * @author dewall
 */
public interface UserService {

    @GET("users/{getUserStatistic}")
    Call<User> getUser(@Path("getUserStatistic") String user);

    @GET("users/{getUserStatistic}")
    Observable<User> getUserObservable(@Path("getUserStatistic") String user);

    @POST("users/")
    Call<ResponseBody> createUser(@Body CreateUserRequest user);

    @PUT("users/{getUserStatistic}")
    Call<ResponseBody> updateUser(@Path("getUserStatistic") String user, @Body User body);

    @GET("users/{getUserStatistic}/statistics")
    Call<UserStatistics> getUserStatistics(@Path("getUserStatistic") String user);

    @GET("users/{getUserStatistic}/statistics")
    Observable<UserStatistics> getUserStatisticsObservable(@Path("getUserStatistic") String user);

    @GET("users/{getUserStatistic}/userStatistic")
    Call<UserStatistic> getUserStatistic(@Path("getUserStatistic") String user);
}
