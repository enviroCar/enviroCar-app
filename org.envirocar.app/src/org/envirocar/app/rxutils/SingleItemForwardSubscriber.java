package org.envirocar.app.rxutils;

import rx.Subscriber;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class SingleItemForwardSubscriber<T> extends ItemForwardSubscriber<T> {
    public static <T> SingleItemForwardSubscriber<T> create(Subscriber<T> subscriber) {
        return new SingleItemForwardSubscriber<>(subscriber);
    }

    /**
     * Constructor
     *
     * @param subscriber
     */
    private SingleItemForwardSubscriber(Subscriber<T> subscriber) {
        super(subscriber);
    }

    @Override
    public void onNext(T t) {
        super.onNext(t);
        subscriber.onCompleted();
    }
}
