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
package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.google.common.base.Preconditions;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class PreferencesHandler implements PreferenceConstants {

    public static final boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;
    public static final boolean DEFAULT_DISPLAY_STAYS_ACTIVE = false;
    public static final boolean DEFAULT_TEXT_TO_SPEECH = false;


    public static boolean isAutoconnectEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_BLUETOOTH_AUTOCONNECT, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getAutoconnectObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getBoolean(PREF_BLUETOOTH_AUTOCONNECT, DEFAULT_BLUETOOTH_AUTOCONNECT)
                .asObservable();
    }

    public static int getDiscoveryInterval(Context context) {
        return getSharedPreferences(context)
                .getInt(PREF_BLUETOOTH_DISCOVERY_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);
    }

    public static Observable<Integer> getDiscoveryIntervalObservable(Context context) {
        return getRxSharedPreferences(context)
                .getInteger(PREF_BLUETOOTH_DISCOVERY_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL)
                .asObservable();
    }

    public static boolean isDisplayStaysActive(Context context) {
        return getSharedPreferences(context)
                .getBoolean(DISPLAY_STAYS_ACTIV, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getDisplayStaysActiveObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(DISPLAY_STAYS_ACTIV, DEFAULT_DISPLAY_STAYS_ACTIVE)
                .asObservable();
    }

    public static boolean isTextToSpeechEnabled(Context context){
        return getSharedPreferences(context)
                .getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH);
    }

    public static Observable<Boolean> getTextToSpeechObservable(Context context){
        return getRxSharedPreferences(context)
                .getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH)
                .asObservable();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        Preconditions.checkNotNull(context, "Input context cannot be null.");
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static RxSharedPreferences getRxSharedPreferences(Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context));
    }

    public static Observable<Long> getRxSharedSamplingRate(Context context) {
        return getRxSharedPreferences(context)
                .getString(SAMPLING_RATE, "5")
                .asObservable().map(s -> Long.parseLong(s));
    }

    public static Long getSamplingRate(Context context) {
        return Long.parseLong(getSharedPreferences(context)
                .getString(SAMPLING_RATE, "5"));
    }
}
