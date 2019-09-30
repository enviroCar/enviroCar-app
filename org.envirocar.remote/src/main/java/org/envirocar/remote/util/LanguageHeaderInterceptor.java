package org.envirocar.remote.util;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author dewall
 */
@Singleton
public class LanguageHeaderInterceptor implements Interceptor {

    @Inject
    public LanguageHeaderInterceptor() {}

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request newRequest = chain.request()
                .newBuilder()
                .addHeader("Accept-Language", Locale.getDefault().toLanguageTag())
                .build();
        return chain.proceed(newRequest);
    }
}
