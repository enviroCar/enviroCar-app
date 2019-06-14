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



import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackStatistics;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * @author dewall
 */
public interface TrackService {

    @GET("users/{user}/tracks/{track}")
    Call<Track> getTrack(@Path("user") String user,
                         @Path("track") String track);

    @GET("users/{user}/tracks/{track}?limit=10000")
    Observable<Track> getTrackObservable(@Path("user") String user,
                                         @Path("track") String track);

    @GET("users/{user}/tracks/{track}?limit=100")
    Call<List<Track>> getTracks(@Path("user") String user,
                                @Path("track") String track);

    @GET("users/{user}/tracks/{track}?limit=100")
    Observable<List<Track>> getTracksObservable(@Path("user") String user,
                                                @Path("track") String track);

    @GET("users/{user}/tracks/{track}")
    Call<List<Track>> getTracks(@Path("user") String user,
                                @Path("track") String track,
                                @Query("limit") int pageSize);

    @GET("users/{user}/tracks/{track}")
    Observable<List<Track>> getTracksObservable(@Path("user") String user,
                                                @Path("track") String track,
                                                @Query("limit") int pageSize);

    @GET("users/{user}/tracks/")
    Call<List<Track>> getTrackIds(@Path("user") String user);

    @GET("users/{user}/tracks/")
    Call<List<Track>> getTrackIdsWithLimit(@Path("user") String user,
                                           @Query("limit") int limit);

    @GET("tracks?limit=1")
    Call<ResponseBody> getAllTracksCount();

    @GET("users/{user}/tracks?limit=1")
    Call<ResponseBody> getAllTracksCountOfUser(@Path("user") String user);

    @POST("users/{user}/tracks/")
    Call<ResponseBody> uploadTrack(@Path("user") String user, @Body Track track);

    @DELETE("users/{user}/tracks/{track}")
    Call<ResponseBody> deleteTrack(@Path("user") String user, @Path("track") String track);

    @GET("users/{user}/tracks")
    Call<List<Track>> getTracksInPeriod(@Path("user") String user,
                                         @Query("during") String during);

    @GET("users/{user}/tracks/{track}/statistics")
    Call<TrackStatistics> getTrackStatistics(@Path("user") String user,
                                             @Path("track") String trackId);

    @GET("users/{user}/tracks/{track}/statistics/{phenomenon}")
    Call<TrackStatistics> getTrackStatisticsByPhenomenon(@Path("user") String user,
                                                        @Path("track") String trackId,
                                                        @Path("phenomenon") String phenomenon);

}

