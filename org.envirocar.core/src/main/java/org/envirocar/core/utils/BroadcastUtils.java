/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BroadcastUtils {

    public static final Observable<Intent> createBroadcastObservable(
            final Context context, final IntentFilter intentFilter) {
        return Observable.create(new Observable.OnSubscribe<Intent>() {
            @Override
            public void call(Subscriber<? super Intent> subscriber) {
                // Start it
                subscriber.onStart();

                // Broadcast receiver for the specific intentfilter
                final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        subscriber.onNext(intent);
                    }
                };

                // create a new subscription that should get called when this observable gets
                // unsubscribed.
                final Subscription subscription = Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        context.unregisterReceiver(broadcastReceiver);
                    }
                });
                subscriber.add(subscription);

                // Register the created broadcastreceiver
                context.registerReceiver(broadcastReceiver, intentFilter, null, null);
            }
        });
    }
}
