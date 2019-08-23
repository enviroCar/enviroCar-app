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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BroadcastUtils {

    public static final Observable<Intent> createBroadcastObservable(final Context context, final IntentFilter intentFilter) {
        return Observable.create(new ObservableOnSubscribe<Intent>() {
            @Override
            public void subscribe(ObservableEmitter<Intent> emitter) throws Exception {
                try {
                    // Broadcast receiver for the specific intentfilter
                    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            emitter.onNext(intent);
                        }
                    };

                    // create a new subscription that should get called when this observable gets
                    // unsubscribed.
                    emitter.setCancellable(() -> context.unregisterReceiver(broadcastReceiver));

                    // Register the created broadcastreceiver
                    context.registerReceiver(broadcastReceiver, intentFilter, null, null);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }
}
