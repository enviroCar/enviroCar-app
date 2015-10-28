package org.envirocar.remote.service;

import com.squareup.okhttp.ResponseBody;


import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;

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

    @GET("users/{user}")
    Call<User> getUser(@Path("user") String user);

    @GET("users/{user}")
    Observable<User> getUserObservable(@Path("user") String user);

    @POST("users/")
    Call<ResponseBody> createUser(@Body User user);

    @PUT("users/{user}")
    Call<ResponseBody> updateUser(@Path("user") String user, @Body User body);

    @GET("users/{user}/statistics")
    Call<UserStatistics> getUserStatistics(@Path("user") String user);

    @GET("users/{user}/statistics")
    Observable<UserStatistics> getUserStatisticsObservable(@Path("user") String user);
}
