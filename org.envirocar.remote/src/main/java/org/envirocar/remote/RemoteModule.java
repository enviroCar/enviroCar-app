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
import org.envirocar.core.util.InjectApplicationScope;
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
@Module
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
        client.interceptors().add(jsonInterceptor);
        client.setConnectTimeout(300, TimeUnit.SECONDS); // connect timeout
        client.setReadTimeout(300, TimeUnit.SECONDS);    // socket timeout
        client.setWriteTimeout(300, TimeUnit.SECONDS);   // write timeout
        return client;
    }

    @Provides
    @Singleton
    protected UserSerializer provideUserSerializer(){
        return new UserSerializer();
    }

    @Provides
    @Singleton
    protected UserStatisticDeserializer provideUserStatisticDeserializer(){
        return new UserStatisticDeserializer();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Car>> provideCarListTypeToken(){
        return new TypeToken<List<Car>>(){};
    }

    @Provides
    @Singleton
    protected CarListDeserializer provideCarListDeserializer(){
        return new CarListDeserializer();
    }

    @Provides
    @Singleton
    protected TrackSerializer provideTrackSerializer(){
        return new TrackSerializer();
    }

    @Provides
    @Singleton
    protected MeasurementSerializer provideMeasurementSerializer(){
        return new MeasurementSerializer();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Track>> provideTrackListTypeToken(){
        return new TypeToken<List<Track>>(){};
    }

    @Provides
    @Singleton
    protected RemoteTrackListDeserializer provideRemoteTrackListDeserializer(){
        return new RemoteTrackListDeserializer();
    }

    @Provides
    @Singleton
    protected TermsOfUseSerializer provideTermsOfUseSerializer(){
        return new TermsOfUseSerializer();
    }

    @Provides
    @Singleton
    protected TypeToken<List<TermsOfUse>> provideTermsOfUseListTypeToken(){
        return new TypeToken<List<TermsOfUse>>(){};
    }

    @Provides
    @Singleton
    protected TermsOfUseListSerializer provideTermsOfUseListSerializer(){
        return new TermsOfUseListSerializer();
    }

    @Provides
    @Singleton
    protected AnnouncementSerializer provideAnnouncementSerializer(){
        return new AnnouncementSerializer();
    }

    @Provides
    @Singleton
    protected FuelingSerializer provideFuelingSerializer(){
        return new FuelingSerializer();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Fueling>> provideFuelingListTypeToken(){
        return new TypeToken<List<Fueling>>(){};
    }

    @Provides
    @Singleton
    protected FuelingListSerializer provideFuelingListSerializer(){
        return new FuelingListSerializer();
    }

    @Provides
    @Singleton
    protected Gson provideGson(UserSerializer userSerializer,UserStatisticDeserializer userStatisticDeserializer,TypeToken<List<Car>> carListTypeToken,
    CarListDeserializer carListDeserializer, TrackSerializer trackSerializer, MeasurementSerializer measurementSerializer,
    TypeToken<List<Track>> trackListTypeToken, RemoteTrackListDeserializer remoteTrackListDeserializer, TermsOfUseSerializer termsOfUseSerializer,
    TypeToken<List<TermsOfUse>> termsOfUseListTypeToken, TermsOfUseListSerializer termsOfUseListSerializer, AnnouncementSerializer announcementSerializer,
    FuelingSerializer fuelingSerializer, TypeToken<List<Fueling>> fuelingListTypeToken, FuelingListSerializer fuelingListSerializer) {
        return new GsonBuilder()
                .registerTypeAdapter(User.class, userSerializer)
                .registerTypeAdapter(UserStatistics.class, userStatisticDeserializer)
                .registerTypeAdapter(Car.class, new CarSerializer())
                .registerTypeAdapter(carListTypeToken.getType(), carListDeserializer)
                .registerTypeAdapter(Track.class, trackSerializer)
                .registerTypeAdapter(Measurement.class, measurementSerializer)
                .registerTypeAdapter(trackListTypeToken.getType(), remoteTrackListDeserializer)
                .registerTypeAdapter(TermsOfUse.class, termsOfUseSerializer)
                .registerTypeAdapter(termsOfUseListTypeToken.getType(), termsOfUseListSerializer)
                .registerTypeAdapter(Announcement.class, announcementSerializer)
                .registerTypeAdapter(Fueling.class, fuelingSerializer)
                .registerTypeAdapter(fuelingListTypeToken.getType(), fuelingListSerializer)
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
