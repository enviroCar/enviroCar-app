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
 *//*

package org.envirocar.app.rxutils;


import org.reactivestreams.Subscriber;

import io.reactivex.ObservableEmitter;

*/
/**
 * @author dewall
 *//*

public class ItemForwardSubscriber<T> implements ObservableEmitter<T> {
    public static <T> ItemForwardSubscriber<T> create(ObservableEmitter<T> subscriber) {
        return new ItemForwardSubscriber<>(subscriber);
    }

    protected final Subscriber<T> subscriber;

    */
/**
     * Constructor
     *
     * @param subscriber
     *//*

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
*/
