package org.envirocar.app.model.service;

import org.envirocar.app.model.User;

import retrofit.Call;
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

    @GET("users/{user}")
    Call<User> getUser(@Path("user") String user);

    @GET("users/{user}")
    Observable<User> getUserObservable(@Path("user") String user);

    @POST("users/")
    Call<User> createUser(User user);

    @PUT("users/")
    Call<User> updateUser(User user);
}
