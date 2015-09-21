package org.envirocar.app.model.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;

import org.envirocar.app.application.UserManager;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.service.gsonutils.CarListDeserializer;
import org.envirocar.app.model.service.gsonutils.CarSerializer;
import org.envirocar.app.model.service.utils.AuthenticationInterceptor;
import org.envirocar.app.model.service.utils.JsonContentTypeInterceptor;

import java.util.List;

import javax.inject.Inject;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * @author dewall
 */
public class EnviroCarService {
    public static final String BASE_URL = "https://envirocar.org/api/stable/";

    @Inject
    protected static UserManager mUsermanager;

    public static UserService getUserService() {
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UserService.class);
    }

    public static CarService getCarService() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Car.class, new CarSerializer())
                .registerTypeAdapter(new TypeToken<List<Car>>() {
                }.getType(), new CarListDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));
        client.interceptors().add(new JsonContentTypeInterceptor());

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(CarService.class);
    }
}
