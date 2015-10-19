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
