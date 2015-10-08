package org.envirocar.remote.service;

import com.squareup.okhttp.ResponseBody;


import org.envirocar.core.entity.Track;

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
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

    @GET("tracks?limit=1")
    Call<ResponseBody> getAllTracksCount();

    @GET("users/{user}/tracks?limit=1")
    Call<ResponseBody> getAllTracksCountOfUser(@Path("user") String user);

    @POST("users/{user}/tracks/")
    Call<ResponseBody> uploadTrack(@Path("user") String user, @Body Track track);

    @DELETE("users/{user}/tracks/{track}")
    Call<ResponseBody> deleteTrack(@Path("user") String user, @Path("track") String track);


}

