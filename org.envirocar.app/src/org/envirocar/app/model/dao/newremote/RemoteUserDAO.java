package org.envirocar.app.model.dao.newremote;

import org.envirocar.app.model.User;

import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

/**
 * @author dewall
 */
public interface RemoteUserDAO extends UserDAO {

    @GET("/users/{user]")
    public User getUser(@Path("user") String name);

    @GET("/users/{user]")
    public Observable<User> getUserObservable(@Path("user") String name);

    @PUT("/users/{user}")
    public void updateUser(User user);

    public void createUser(User user);

}
