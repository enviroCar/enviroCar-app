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
}
