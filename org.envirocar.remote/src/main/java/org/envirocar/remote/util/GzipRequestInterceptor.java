/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.remote.util;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
                    public long contentLength() {
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
