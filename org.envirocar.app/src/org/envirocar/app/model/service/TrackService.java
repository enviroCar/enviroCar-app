package org.envirocar.app.model.service;

import org.envirocar.app.storage.Track;

import retrofit.Call;
import retrofit.http.DELETE;
import retrofit.http.Path;

/**
 * @author dewall
 */
public interface TrackService {

    //    @GET("users/{user}/tracks/{track}")
    //    Track getTrack();

    @DELETE("users/{user}/tracks/{track}")
    Call<Track> deleteTrack(@Path("user") String user, @Path("track") String trackID);
}

