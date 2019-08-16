package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author dewall
 */
public abstract class AbstractCachable<T> {

    private final Context context;
    private final String prefKey;

    /**
     * Constructor.
     */
    public AbstractCachable(Context context, String prefKey) {
        this.context = context;
        this.prefKey = prefKey;
    }

    protected abstract T readFromCache();

    protected abstract void writeToCache(T t);

    protected SharedPreferences getSharedPreferences() {
        SharedPreferences prefs = context.getSharedPreferences(this.prefKey, Context.MODE_PRIVATE);
        return prefs;
    }

}
