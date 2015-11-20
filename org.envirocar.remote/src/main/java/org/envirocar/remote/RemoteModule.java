/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.remote;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;

import org.envirocar.core.ContextInternetAccessProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.remote.dao.CacheAnnouncementsDAO;
import org.envirocar.remote.dao.CacheCarDAO;
import org.envirocar.remote.dao.CacheFuelingDAO;
import org.envirocar.remote.dao.CacheTermsOfUseDAO;
import org.envirocar.remote.dao.CacheTrackDAO;
import org.envirocar.remote.dao.CacheUserDAO;
import org.envirocar.remote.dao.RemoteAnnouncementsDAO;
import org.envirocar.remote.dao.RemoteCarDAO;
import org.envirocar.remote.dao.RemoteFuelingDAO;
import org.envirocar.remote.dao.RemoteTermsOfUseDAO;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.dao.RemoteUserDAO;
import org.envirocar.remote.dao.RemoteUserStatisticsDAO;
import org.envirocar.remote.serializer.AnnouncementSerializer;
import org.envirocar.remote.serializer.CarListDeserializer;
import org.envirocar.remote.serializer.CarSerializer;
import org.envirocar.remote.serializer.FuelingListSerializer;
import org.envirocar.remote.serializer.FuelingSerializer;
import org.envirocar.remote.serializer.MeasurementSerializer;
import org.envirocar.remote.serializer.RemoteTrackListDeserializer;
import org.envirocar.remote.serializer.TermsOfUseListSerializer;
import org.envirocar.remote.serializer.TermsOfUseSerializer;
import org.envirocar.remote.serializer.TrackSerializer;
import org.envirocar.remote.serializer.UserSerializer;
import org.envirocar.remote.serializer.UserStatisticDeserializer;
import org.envirocar.remote.service.AnnouncementsService;
import org.envirocar.remote.service.CarService;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.FuelingService;
import org.envirocar.remote.service.TermsOfUseService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.AuthenticationInterceptor;
import org.envirocar.remote.util.JsonContentTypeInterceptor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {
                DAOProvider.class,
                CacheUserDAO.class,
                CacheCarDAO.class,
                CacheFuelingDAO.class,
                CacheTermsOfUseDAO.class,
                CacheTrackDAO.class,
                CacheAnnouncementsDAO.class,
                RemoteAnnouncementsDAO.class,
                RemoteFuelingDAO.class,
                RemoteCarDAO.class,
                RemoteTermsOfUseDAO.class,
                RemoteTrackDAO.class,
                RemoteUserDAO.class,
                RemoteUserStatisticsDAO.class,
                DAOProvider.class
        },
        staticInjections = EnviroCarService.class
)
public class RemoteModule {
    public static HttpUrl URL_ENVIROCAR_BASE = HttpUrl.parse(EnviroCarService.BASE_URL);

    /**
     * Provides the InternetAccessProivder.
     *
     * @return the provider for internet access.
     */
    @Provides
    @Singleton
    public InternetAccessProvider provideInternetAccessProvider(
            @InjectApplicationScope Context context) {
        return new ContextInternetAccessProvider(context);
    }

    @Provides
    @Singleton
    protected HttpUrl provideBaseUrl() {
        return URL_ENVIROCAR_BASE;
    }

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient(AuthenticationInterceptor authInterceptor,
                                               JsonContentTypeInterceptor jsonInterceptor) {
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(authInterceptor);
        client.interceptors().add(new JsonContentTypeInterceptor());
        client.setConnectTimeout(300, TimeUnit.SECONDS); // connect timeout
        client.setReadTimeout(300, TimeUnit.SECONDS);    // socket timeout
        client.setWriteTimeout(300, TimeUnit.SECONDS);   // write timeout
        return client;
    }

    @Provides
    @Singleton
    protected Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(User.class, new UserSerializer())
                .registerTypeAdapter(UserStatistics.class, new UserStatisticDeserializer())
                .registerTypeAdapter(Car.class, new CarSerializer())
                .registerTypeAdapter(new TypeToken<List<Car>>() {
                }.getType(), new CarListDeserializer())
                .registerTypeAdapter(Track.class, new TrackSerializer())
                .registerTypeAdapter(Measurement.class, new MeasurementSerializer())
                .registerTypeAdapter(new TypeToken<List<Track>>() {
                }.getType(), new RemoteTrackListDeserializer())
                .registerTypeAdapter(TermsOfUse.class, new TermsOfUseSerializer())
                .registerTypeAdapter(new TypeToken<List<TermsOfUse>>() {
                }.getType(), new TermsOfUseListSerializer())
                .registerTypeAdapter(Announcement.class, new AnnouncementSerializer())
                .registerTypeAdapter(Fueling.class, new FuelingSerializer())
                .registerTypeAdapter(new TypeToken<List<Fueling>>() {
                }.getType(), new FuelingListSerializer())
                .create();
    }

    @Provides
    @Singleton
    protected Retrofit provideRetrofit(HttpUrl baseUrl, OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    protected UserService provideUserService(Retrofit retrofit) {
        return retrofit.create(UserService.class);
    }

    @Provides
    @Singleton
    protected CarService provideCarService(Retrofit retrofit) {
        return retrofit.create(CarService.class);
    }

    @Provides
    @Singleton
    protected TrackService provideTrackService(Retrofit retrofit) {
        return retrofit.create(TrackService.class);
    }

    @Provides
    @Singleton
    protected TermsOfUseService provideTermsOfUseService(Retrofit retrofit) {
        return retrofit.create(TermsOfUseService.class);
    }

    @Provides
    @Singleton
    protected FuelingService provideFuelingService(Retrofit retrofit) {
        return retrofit.create(FuelingService.class);
    }

    @Provides
    @Singleton
    protected AnnouncementsService provideAnnouncementService(Retrofit retrofit) {
        return retrofit.create(AnnouncementsService.class);
    }
}
