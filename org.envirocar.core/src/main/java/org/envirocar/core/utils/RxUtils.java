/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.utils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RxUtils {
    public static <T> Observable<List<T>> debouncedBuffer(Observable<T> observable, final int timeout) {
        return observable.publish(stream -> stream.buffer(stream.debounce(timeout, TimeUnit.MILLISECONDS)));
    }

    public static <T extends Comparable<T>> Observable<List<T>> sortList(
            Observable<List<T>> observable) {
        return observable.map(ts -> {
            Collections.sort(ts);
            return ts;
        });
    }
}
