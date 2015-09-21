package org.envirocar.app.model.service.utils;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * @author dewall
 */
public class JsonContentTypeInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request newRequest = chain.request()
                .newBuilder()
                .addHeader("Content-Type", "application/json")
                .build();
        return chain.proceed(newRequest);
    }
}
