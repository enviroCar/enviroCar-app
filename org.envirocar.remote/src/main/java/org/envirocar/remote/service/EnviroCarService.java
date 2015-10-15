package org.envirocar.remote.service;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.envirocar.core.UserManager;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.remote.serializer.AnnouncementSerializer;
import org.envirocar.remote.serializer.CarListDeserializer;
import org.envirocar.remote.serializer.CarSerializer;
import org.envirocar.remote.serializer.MeasurementSerializer;
import org.envirocar.remote.serializer.RemoteTrackListDeserializer;
import org.envirocar.remote.serializer.TermsOfUseListSerializer;
import org.envirocar.remote.serializer.TermsOfUseSerializer;
import org.envirocar.remote.serializer.TrackSerializer;
import org.envirocar.remote.serializer.UserSerializer;
import org.envirocar.remote.serializer.UserStatisticDeserializer;
import org.envirocar.remote.util.AuthenticationInterceptor;
import org.envirocar.remote.util.JsonContentTypeInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * @author dewall
 */
public class EnviroCarService {
    public static final String BASE_URL = "https://envirocar.org/api/dev/";

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
        client.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                return chain.proceed(request);
            }
        });

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
                .registerTypeAdapter(new TypeToken<List<Track>>() {
                }.getType(), new RemoteTrackListDeserializer())
                .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));
        client.interceptors().add(new JsonContentTypeInterceptor());
        client.setConnectTimeout(300, TimeUnit.SECONDS); // connect timeout
        client.setReadTimeout(300, TimeUnit.SECONDS);    // socket timeout
        client.setWriteTimeout(300, TimeUnit.SECONDS);   // write timeout

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TrackService.class);
    }

    public static TermsOfUseService getTermsOfUseService() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(TermsOfUse.class, new TermsOfUseSerializer())
                .registerTypeAdapter(new TypeToken<List<TermsOfUse>>() {
                }.getType(), new TermsOfUseListSerializer())
                .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TermsOfUseService.class);
    }

    public static FuelingService getFuelingService() {
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AuthenticationInterceptor(mUsermanager));

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(FuelingService.class);
    }

    public static AnnouncementsService getAnnouncementService() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Announcement.class, new AnnouncementSerializer())
                .create();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(AnnouncementsService.class);
    }
}
