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
package org.envirocar.app.handler.preferences;

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
    protected final T readFromCache() {
        return this.readFromCache(getSharedPreferences());
    }

    /**
     * Abstract method to read from the cache.
     *
     * @param preferences the shared preferences
     * @return
     */
    protected abstract T readFromCache(SharedPreferences preferences);

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
        this.getSharedPreferences().edit().clear().commit();
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
