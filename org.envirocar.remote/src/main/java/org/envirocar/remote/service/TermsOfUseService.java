package org.envirocar.remote.service;



import org.envirocar.core.entity.TermsOfUse;

import java.util.List;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * @author dewall
 */
public interface TermsOfUseService {

    @GET("termsOfUse")
    Call<List<TermsOfUse>> getAllTermsOfUse();

    @GET("termsOfUse/{id}")
    Call<TermsOfUse> getTermsOfUseByID(@Path("id") String id);

}
