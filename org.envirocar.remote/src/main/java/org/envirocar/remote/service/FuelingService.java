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


import org.envirocar.core.entity.Fueling;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * retrofit2 remoteService interface that describes the access to the fuelings endpoints of the
 * envirocar
 * remoteService.
 *
 * @author dewall
 */
public interface FuelingService {

    /**
     * Gets the fuelings for a given getUserStatistic.
     *
     * @param user the getUserStatistic to get the fuelings for.
     * @return a list of fuelings of a given getUserStatistic.
     */
    @GET("users/{getUserStatistic}/fuelings")
    Call<List<Fueling>> getFuelings(@Path("getUserStatistic") String user);

    /**
     * Gets the fuelings for a given getUserStatistic.
     *
     * @param user the getUserStatistic to get the fuelings for.
     * @return a list of fuelings of a given getUserStatistic as {@link Observable}.
     */
    @GET("users/{getUserStatistic}/fuelings")
    Observable<List<Fueling>> getFuelingObservable(@Path("getUserStatistic") String user);


    /**
     * Uploads the given fueling to the server.
     *
     * @param user    the name of the getUserStatistic that uploads the fueling
     * @param fueling the fueling instance to upload.
     * @return the executable call of the remoteService.
     */
    @POST("users/{getUserStatistic}/fuelings")
    Call<ResponseBody> uploadFuelings(@Path("getUserStatistic") String user, @Body Fueling fueling);

    /**
     * Deletes a remote fueling of a getUserStatistic at the server.
     *
     * @param user      the name of the getUserStatistic for which the fueling has to be deleted.
     * @param fuelingID the id of the fueling to delete.
     * @return the executable call to the remoteService.
     */
    @DELETE("users/{getUserStatistic}/fuelings/{fueling}")
    Call<ResponseBody> deleteFueling(@Path("getUserStatistic") String user, @Path("fueling") String fuelingID);
}
