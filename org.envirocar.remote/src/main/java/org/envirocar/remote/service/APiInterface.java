package org.envirocar.remote.service;


import org.envirocar.core.entity.*;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface APiInterface {
    @GET("manufacturers")
    Observable<List<manufacturer>> getName();
    @GET("manufacturers/{hsn}/vehicles")
    Observable<List<vehicleModels>> getCommercialName(@Path("hsn") String hsn);
    @GET("manufacturers/{hsn}/vehicles/{tsn}")
    Observable<modelinformation> getcategory(@Path("hsn") String hsn, @Path("tsn") String tsn);


}
