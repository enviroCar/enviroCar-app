package org.envirocar.remote.util;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author dewall
 */
@Singleton
public class JsonContentTypeInterceptor implements Interceptor {

    @Inject
    public JsonContentTypeInterceptor() {}

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request newRequest = chain.request()
                .newBuilder()
                .addHeader("Content-Type", "application/json")
                .build();
        return chain.proceed(newRequest);
    }
}
