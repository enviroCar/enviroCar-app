package org.envirocar.remote.util;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

/**
 * @author dewall
 */
public class GzipRequestInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request original = chain.request();
        if (original.body() == null || original.header("Content-Encoding") != null) {
            return chain.proceed(original);
        }

        Request gzip = original.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(original.method(), new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return original.body().contentType();
                    }

                    @Override
                    public long contentLength() throws IOException {
                        return -1;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        BufferedSink gzipper = Okio.buffer(new GzipSink(sink));
                        original.body().writeTo(gzipper);
                        gzipper.close();
                    }
                })
                .build();
        return chain.proceed(gzip);
    }
}
