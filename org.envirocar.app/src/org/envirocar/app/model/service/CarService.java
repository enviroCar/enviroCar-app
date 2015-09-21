package org.envirocar.app.model.service;

import org.envirocar.app.model.Car;

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
    String KEY_ROOT = "sensors";
    String KEY_CAR = "properties";
    String KEY_CAR_TYPE = "type";
    String KEY_CAR_MODEL = "model";
    String KEY_CAR_ID = "id";
    String KEY_CAR_FUELTYPE = "fuelType";
    String KEY_CAR_CONSTRUCTIONYEAR = "constructionYear";
    String KEY_CAR_MANUFACTURER = "manufacturer";
    String KEY_CAR_ENGINEDISPLACEMENT = "engineDisplacement";

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