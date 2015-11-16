package org.envirocar.remote.service;

import com.squareup.okhttp.ResponseBody;

import org.envirocar.core.entity.Fueling;

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;

/**
 * Retrofit remoteService interface that describes the access to the fuelings endpoints of the
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
