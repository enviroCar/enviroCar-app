package org.envirocar.app.model.dao.service;

import org.envirocar.app.model.TermsOfUse;

import retrofit.Call;
import retrofit.http.GET;

/**
 * @author dewall
 */
public interface TermsOfUseService {

    @GET("termsOfUse")
    Call<TermsOfUse>
}
