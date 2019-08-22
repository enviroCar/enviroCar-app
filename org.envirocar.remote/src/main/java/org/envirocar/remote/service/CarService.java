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
package org.envirocar.remote.service;


import org.envirocar.core.entity.Car;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * @author dewall
 */
public interface CarService {

    @GET("sensors/")
    Call<List<Car>> getAllCars();

    @GET("sensors/")
    Observable<List<Car>> getAllCarsObservable();

    @GET("sensors/")
    Call<List<Car>> getAllCars(@Query("page") int page);

    @GET("sensors/")
    Observable<List<Car>> getAllCarsObservable(@Query("page") int page);

    @GET("users/{user}/sensors/")
    Call<List<Car>> getAllCars(@Path("user") String user);

    @POST("sensors")
    Call<Car> createCar(@Body Car car);
}
