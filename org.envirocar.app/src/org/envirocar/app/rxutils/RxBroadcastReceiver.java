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
package org.envirocar.app.rxutils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.envirocar.core.logging.Logger;

import java.lang.ref.WeakReference;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 * @author dewall
 */
public class RxBroadcastReceiver implements ObservableOnSubscribe<Intent>, Disposable {
    private static final Logger LOG = Logger.getLogger(RxBroadcastReceiver.class);

    private final WeakReference<Context> weakContext;
    private final IntentFilter intentFilter;

    //
    private Emitter<Intent> emitter;
    private BroadcastReceiver broadcastReceiver;


    public static Observable<Intent> create(Context context, IntentFilter intentFilter) {
        return Observable.defer(() -> Observable.create(new RxBroadcastReceiver(context, intentFilter)));
    }

    /**
     * Prviate constructor.
     *
     * @param context
     * @param intentFilter
     */
    private RxBroadcastReceiver(Context context, IntentFilter intentFilter) {
        this.weakContext = new WeakReference<>(context.getApplicationContext());
        this.intentFilter = intentFilter;
    }

    @Override
    public void subscribe(ObservableEmitter<Intent> emitter) throws Exception {
        this.emitter = emitter;
        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LOG.info("RECEIVED INTENT", intent.getAction());
                emitter.onNext(intent);
            }
        };

        if (weakContext != null && weakContext.get() != null) {
            weakContext.get().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    @Override
    public void dispose() {
        if (weakContext != null && weakContext.get() != null && broadcastReceiver != null) {
            weakContext.get().unregisterReceiver(broadcastReceiver);
        }
        broadcastReceiver = null;
        emitter = null;
    }

    @Override
    public boolean isDisposed() {
        return broadcastReceiver == null || weakContext == null || weakContext.get() == null;
    }
}
