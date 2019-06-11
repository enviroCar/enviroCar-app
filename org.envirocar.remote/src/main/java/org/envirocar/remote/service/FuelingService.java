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

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * retrofit2 remoteService interface that describes the access to the fuelings endpoints of the
 * envirocar
 * remoteService.
 *
 * @author dewall
 */
public interface FuelingService {

    /**
     * Gets the fuelings for a given user.
     *
     * @param user the user to get the fuelings for.
     * @return a list of fuelings of a given user.
     */
    @GET("users/{user}/fuelings")
    Call<List<Fueling>> getFuelings(@Path("user") String user);

    /**
     * Gets the fuelings for a given user.
     *
     * @param user the user to get the fuelings for.
     * @return a list of fuelings of a given user as {@link Observable}.
     */
    @GET("users/{user}/fuelings")
    Observable<List<Fueling>> getFuelingObservable(@Path("user") String user);


    /**
     * Uploads the given fueling to the server.
     *
     * @param user    the name of the user that uploads the fueling
     * @param fueling the fueling instance to upload.
     * @return the executable call of the remoteService.
     */
    @POST("users/{user}/fuelings")
    Call<ResponseBody> uploadFuelings(@Path("user") String user, @Body Fueling fueling);

    /**
     * Deletes a remote fueling of a user at the server.
     *
     * @param user      the name of the user for which the fueling has to be deleted.
     * @param fuelingID the id of the fueling to delete.
     * @return the executable call to the remoteService.
     */
    @DELETE("users/{user}/fuelings/{fueling}")
    Call<ResponseBody> deleteFueling(@Path("user") String user, @Path("fueling") String fuelingID);
}
