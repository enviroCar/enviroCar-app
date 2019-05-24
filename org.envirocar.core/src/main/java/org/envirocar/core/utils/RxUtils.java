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
package org.envirocar.core.utils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RxUtils {
    public static <T> Observable<List<T>> debouncedBuffer(Observable<T> observable, final int
            timeout) {
        return observable.publish(new Func1<Observable<T>, Observable<List<T>>>() {
            @Override
            public Observable<List<T>> call(Observable<T> stream) {
                return stream.buffer(stream.debounce(timeout, TimeUnit.MILLISECONDS));
            }
        });
    }

    public static <T extends Comparable<T>> Observable<List<T>> sortList(
            Observable<List<T>> observable) {
        return observable.map(new Func1<List<T>, List<T>>() {
            @Override
            public List<T> call(List<T> ts) {
                Collections.sort(ts);
                return ts;
            }
        });
    }
}
