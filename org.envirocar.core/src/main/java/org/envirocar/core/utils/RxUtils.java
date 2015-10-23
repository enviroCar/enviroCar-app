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
