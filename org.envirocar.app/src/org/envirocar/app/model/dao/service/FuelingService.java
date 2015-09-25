package org.envirocar.app.model.dao.service;

import com.squareup.okhttp.ResponseBody;

import org.envirocar.app.model.Fueling;

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;

/**
 * Retrofit service interface that describes the access to the fuelings endpoints of the envirocar
 * service.
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
     * @return the executable call of the service.
     */
    @POST("users/{user}/fuelings")
    Call<ResponseBody> uploadFuelings(@Path("user") String user, @Body Fueling fueling);
}
