package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;

import org.envirocar.app.exception.CacheEmptyException;

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

    /**
     * Method to read from the cache.
     *
     * @return object read from the cache.
     */
    protected final T readFromCache() throws CacheEmptyException {
        return this.readFromCache(getSharedPreferences());
    }

    /**
     * Abstract method to read from the cache.
     *
     * @param preferences the shared preferences
     * @return
     */
    protected abstract T readFromCache(SharedPreferences preferences) throws CacheEmptyException;

    /**
     * Abstract method to write to the cache.
     *
     * @param t
     */
    protected final void writeToCache(T t) {
        this.writeToCache(t, getSharedPreferences());
    }

    /**
     * Abstract method to write to the cache.
     *
     * @param t
     */
    protected abstract void writeToCache(T t, SharedPreferences preferences);

    /**
     * Resets the cache.
     */
    protected void resetCache() {
        this.getSharedPreferences().edit().clear();
    }

    /**
     * Returns the shared preferences.
     *
     * @return the shared preferences.
     */
    protected SharedPreferences getSharedPreferences() {
        SharedPreferences prefs = context.getSharedPreferences(this.prefKey, Context.MODE_PRIVATE);
        return prefs;
    }

}
