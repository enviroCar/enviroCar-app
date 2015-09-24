package org.envirocar.app.model.dao.service;

import com.squareup.okhttp.ResponseBody;

import org.envirocar.app.model.User;
import org.envirocar.app.model.UserStatistics;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

/**
 * @author dewall
 */
public interface UserService {
    String KEY_USER_NAME = "name";
    String KEY_USER_MAIL = "mail";
    String KEY_USER_TOKEN = "token";
    String KEY_USER_TOU_VERSION = "touVersion";

    String KEY_STATISTICS = "statistics";
    String KEY_STATISTICS_MAX = "max";
    String KEY_STATISTICS_MIN = "min";
    String KEY_STATISTICS_AVG = "avg";
    String KEY_STATISTICS_MEASUREMENTS = "measurements";
    String KEY_STATISTICS_TRACKS = "tracks";
    String KEY_STATISTICS_SENSORS = "sensors";
    String KEY_STATISTICS_PHENOMENON = "phenomenon";
    String KEY_STATISTICS_PHENOMENON_NAME = "name";
    String KEY_STATISTICS_PHENOMENON_UNIT = "unit";


    @GET("users/{user}")
    Call<User> getUser(@Path("user") String user);

    @GET("users/{user}")
    Observable<User> getUserObservable(@Path("user") String user);

    @POST("users/")
    Call<ResponseBody> createUser(@Body User user);

    @PUT("users/")
    Call<ResponseBody> updateUser(@Body User user);

    @GET("users/{user}/statistics")
    Call<UserStatistics> getUserStatistics(@Path("user") String user);

    @GET("users/{user}/statistics")
    Observable<UserStatistics> getUserStatisticsObservable(@Path("user") String user);
}
