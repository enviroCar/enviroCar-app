/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.remote.injection.modules;

import com.google.gson.Gson;

import org.envirocar.remote.service.AnnouncementsService;
import org.envirocar.remote.service.CarService;
import org.envirocar.remote.service.FuelingService;
import org.envirocar.remote.service.PrivacyStatementService;
import org.envirocar.remote.service.TermsOfUseService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.service.UserService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit specific injection module
 *
 * @author dewall
 */
@Module
public class RetrofitModule {

    @Provides
    @Singleton
    protected Retrofit provideRetrofit(HttpUrl baseUrl, OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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

    @Provides
    @Singleton
    protected PrivacyStatementService providePrivacyStatement(Retrofit retrofit) {
        return retrofit.create(PrivacyStatementService.class);
    }
}
