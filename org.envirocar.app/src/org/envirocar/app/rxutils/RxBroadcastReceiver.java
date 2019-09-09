package org.envirocar.app.rxutils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

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
