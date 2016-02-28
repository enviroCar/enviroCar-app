package org.envirocar.app.rxutils;

import rx.Subscriber;

/**
 * @author dewall
 */
public class ItemForwardSubscriber<T> extends Subscriber<T> {
    public static <T> ItemForwardSubscriber<T> create(Subscriber<T> subscriber) {
        return new ItemForwardSubscriber<>(subscriber);
    }

    protected final Subscriber<T> subscriber;

    /**
     * Constructor
     *
     * @param subscriber
     */
    protected ItemForwardSubscriber(Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onStart() {
        subscriber.onStart();
    }

    @Override
    public void onCompleted() {
        subscriber.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        subscriber.onError(e);
    }

    @Override
    public void onNext(T t) {
        subscriber.onNext(t);
    }
}
