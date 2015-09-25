package org.envirocar.app.model.dao.service;

import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * @author dewall
 */
public interface TermsOfUseService {
    String KEY_TERMSOFUSE = "termsOfUse";
    String KEY_TERMSOFUSE_ID = "id";
    String KEY_TERMSOFUSE_ISSUEDDATE = "issuedDate";

    @GET("termsOfUse")
    Call<TermsOfUse> getTermsOfUse();

    @GET("termsOfUse/{id}")
    Call<TermsOfUseInstance> getTermsOfUseByID(@Path("id") String id);

}
