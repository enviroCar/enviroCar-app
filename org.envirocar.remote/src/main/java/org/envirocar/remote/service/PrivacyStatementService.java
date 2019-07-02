package org.envirocar.remote.service;

import org.envirocar.core.entity.PrivacyStatement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * @author dewall
 */
public interface PrivacyStatementService {

    @GET("privacyStatement")
    Call<List<PrivacyStatement>> getPrivacyStatements();

    @GET("privacyStatement/{id}")
    Call<PrivacyStatement> getPrivacyStatement(@Path("id") String id);
}
