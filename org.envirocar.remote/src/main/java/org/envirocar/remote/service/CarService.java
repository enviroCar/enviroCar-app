package org.envirocar.remote.service;


import org.envirocar.core.entity.Car;

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 * @author dewall
 */
public interface CarService {

    @GET("sensors/")
    Call<List<Car>> getAllCars();

    @GET("sensors/")
    Call<List<Car>> getAllCars(@Query("page") int page);

    @GET("sensors/")
    Observable<List<Car>> getAllCarsObservable();

    @GET("sensors/")
    Observable<List<Car>> getAllCarsObservable(@Query("page") int page);

    @POST("sensors")
    Call<Car> createCar(@Body Car car);
}