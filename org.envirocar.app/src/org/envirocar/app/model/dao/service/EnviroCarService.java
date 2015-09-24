package org.envirocar.app.model.dao.service;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.envirocar.app.application.UserManager;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.User;
import org.envirocar.app.model.UserStatistics;
import org.envirocar.app.model.dao.service.serializer.CarListDeserializer;
import org.envirocar.app.model.dao.service.serializer.CarSerializer;
import org.envirocar.app.model.dao.service.serializer.MeasurementSerializer;
import org.envirocar.app.model.dao.service.serializer.TrackSerializer;
import org.envirocar.app.model.dao.service.serializer.UserSerializer;
import org.envirocar.app.model.dao.service.serializer.UserStatisticDeserializer;
import org.envirocar.app.model.dao.service.utils.AuthenticationInterceptor;
import org.envirocar.app.model.dao.service.utils.JsonContentTypeInterceptor;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;

import java.io.IOException;
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
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(User.class, new UserSerializer())
                .registerTypeAdapter(UserStatistics.class, new UserStatisticDeserializer())
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
        client.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);
                Log.w("EnviroCarService", response.header("Content-Encoding"));
                return response;
            }
        });

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(CarService.class);
    }

    public static TrackService getTrackService() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Car.class, new CarSerializer())
                .registerTypeAdapter(Track.class, new TrackSerializer())
                .registerTypeAdapter(Measurement.class, new MeasurementSerializer())
                .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));
        client.interceptors().add(new JsonContentTypeInterceptor());
        client.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Log.w("EnviroCarService", request.header("Content-Encoding"));
                return chain.proceed(request);
            }
        });

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TrackService.class);
    }
}
