package org.envirocar.app.model.dao.service;

import com.squareup.okhttp.ResponseBody;

import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;

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
    String KEY_TRACK_TYPE = "type";
    String KEY_TRACK_PROPERTIES = "properties";
    String KEY_TRACK_PROPERTIES_ID = "id";
    String KEY_TRACK_PROPERTIES_NAME = "name";
    String KEY_TRACK_PROPERTIES_DESCRIPTION = "description";
    String KEY_TRACK_PROPERTIES_CREATED = "created";
    String KEY_TRACK_PROPERTIES_MODIFIED = "modified";
    String KEY_TRACK_PROPERTIES_SENSOR = "sensor";
    String KEY_TRACK_PROPERTIES_LENGTH = "length";

    String KEY_TRACK_FEATURES = "features";
    String KEY_TRACK_FEATURES_GEOMETRY = "geometry";
    String KEY_TRACK_FEATURES_GEOMETRY_COORDINATES = "coordinates";
    String KEY_TRACK_FEATURES_PROPERTIES = "properties";
    String KEY_TRACK_FEATURES_PROPERTIES_ID = "id";
    String KEY_TRACK_FEATURES_PROPERTIES_TIME = "time";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS = "phenomenons";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_VALUE = "value";
    String KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS_UNIT = "unit";


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
    Call<List<RemoteTrack>> getTrackIds(@Path("user") String user);

    @GET("tracks?limit=1")
    Call<ResponseBody> getAllTracksCount();

    @GET("users/{user}/tracks?limit=1")
    Call<ResponseBody> getAllTracksCountOfUser(@Path("user") String user);

    @POST("users/{user}/tracks/")
    Call<ResponseBody> uploadTrack(@Path("user") String user, @Body Track track);

    @DELETE("users/{user}/tracks/{track}")
    Call<ResponseBody> deleteTrack(@Path("user") String user, @Path("track") String track);


}

