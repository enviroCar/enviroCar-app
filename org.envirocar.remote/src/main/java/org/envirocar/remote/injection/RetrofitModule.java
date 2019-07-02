package org.envirocar.remote.injection;

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
