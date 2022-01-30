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
package org.envirocar.app.handler;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.preference.PreferenceManager;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.recording.RecordingType;

import io.reactivex.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class ApplicationSettings {

    // Default values
    public static final boolean DEFAULT_AUTOMATIC_UPLOAD = false;
    public static final boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;
    public static final boolean DEFAULT_DISPLAY_STAYS_ACTIVE = false;
    public static final boolean DEFAULT_TEXT_TO_SPEECH = false;
    public static final boolean DEFAULT_BLUETOOTH_SERVICE_AUTOSTART = true;
    public static final boolean DEFAULT_PREF_ENABLE_GPS_BASED_TRACK_RECORDING = false;
    public static final boolean DEFAULT_OBFUSCATION = false;
    public static final int DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL = 60;
    public static final int DEFAULT_TRACK_TRIM_DURATION = 110;
    public static final boolean DEFAULT_DEBUG_LOGGING = false;
    public static final int DEFAULT_SAMPLING_RATE = 5;
    public static final String DEFAULT_CAMPAIGN_PROFILE = "DEFAULT_COMANND_PROFILE";

//    // General Settings
//    public static final String PREF_AUTOMATIC_UPLOAD_OF_TRACKS = "pref_automatic_upload_tracks";
//    public static final String PREF_DISPLAY_STAYS_ACTIVE = "pref_display_stays_active";
//    public static final String PREF_USE_IMPERIAL_UNITS = "pref_use_imperial_unit";
//    public static final String PREF_OBFUSCATE_TRACK = "pref_obfuscate_track";
//    public static final String PREF_TEXT_TO_SPEECH = "pref_text_to_speech";
//
//    // Auto-recording Settings
//    public static final String PREF_BACKGROUND_PROCESS = "pref_background_progress";
//    public static final String PREF_AUTOMATIC_RECORDING = "pref_automatic_recording";
//    public static final String PREF_SEARCH_INTERVAL = "pref_search_interval";
//
//    // Optional Settings
//    public static final String PREF_SAMPLING_RATE = "pref_samplingrate";
//    public static final String PREF_DEBUG_LOGGING = "pref_debug_logging";
//    public static final String PREF_DIESEL_ESTIMATION = "pref_diesel_estimation";
//    public static final String PREF_TRACK_TRIM_DURATION = "pref_track_trim_duration";
//    public static final String PREF_GPS_BASED_TRACKING = "pref_gps_based_tracking";

    // General Settings settings
    public static Observable<Boolean> getAutomaticUploadObservable(Context context){
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_automatic_upload), DEFAULT_AUTOMATIC_UPLOAD)
                .asObservable();
    }

    public static boolean isDisplayStaysActive(Context context) {
        return getSharedPreferences(context).getBoolean(s(context, R.string.prefkey_display_always_active), DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getDisplayStaysActiveObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_display_always_active), DEFAULT_DISPLAY_STAYS_ACTIVE)
                .asObservable();
    }

    public static boolean isObfuscationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(s(context, R.string.prefkey_privacy), DEFAULT_OBFUSCATION);
    }

    public static Observable<Boolean> getObfuscationObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_privacy), false)
                .asObservable();
    }

    public static boolean isTextToSpeechEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(s(context, R.string.prefkey_text_to_speech), DEFAULT_TEXT_TO_SPEECH);
    }

    public static Observable<Boolean> getTextToSpeechObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_text_to_speech), DEFAULT_TEXT_TO_SPEECH)
                .asObservable();
    }

    public static boolean isAutorecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_automatic_recording), DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getAutoconnectEnabledObservable(final Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_automatic_recording), DEFAULT_BLUETOOTH_AUTOCONNECT)
                .asObservable();
    }

    public static int getDiscoveryInterval(Context context) {
        return getSharedPreferences(context)
                .getInt(s(context, R.string.prefkey_search_interval), DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);
    }

    public static Observable<Integer> getDiscoveryIntervalObservable(Context context) {
        return getRxSharedPreferences(context)
                .getInteger(s(context, R.string.prefkey_search_interval), DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL)
                .asObservable();
    }

    public static void setDiscoveryInterval(Context context, int discoveryInterval) {
        getSharedPreferences(context)
                .edit()
                .putInt(s(context, R.string.prefkey_search_interval), discoveryInterval)
                .apply();
    }

    // Optional Settings
    public static int getSamplingRate(Context context) {
        return getSharedPreferences(context).getInt(s(context, R.string.prefkey_samplingrate), DEFAULT_SAMPLING_RATE);
    }

    public static void setSamplingRate(Context context, int samplingRate) {
        getSharedPreferences(context)
                .edit()
                .putInt(s(context, R.string.prefkey_samplingrate), samplingRate)
                .apply();
    }

    public static Observable<Integer> getRxSharedSamplingRate(Context context) {
        return getRxSharedPreferences(context)
                .getInteger(s(context, R.string.prefkey_samplingrate), DEFAULT_SAMPLING_RATE)
                .asObservable();
    }

    public static Observable<Boolean> getDebugLoggingObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_enable_debug_logging), DEFAULT_DEBUG_LOGGING)
                .asObservable();
    }

    public static boolean isDieselConsumptionEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(s(context, R.string.prefkey_enable_diesel_consumption), false);
    }

    public static Observable<Boolean> getDieselConsumptionObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(s(context, R.string.prefkey_enable_diesel_consumption), false)
                .asObservable();
    }

    public static Observable<Integer> getTrackTrimDurationObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getInteger(s(context, R.string.prefkey_track_trim_duration), DEFAULT_TRACK_TRIM_DURATION)
                .asObservable();
    }

    public static void setTrackTrimDurationObservable(final Context context, int trackTrimDuration) {
        getSharedPreferences(context).edit()
                .putInt(s(context, R.string.prefkey_track_trim_duration), trackTrimDuration)
                .apply();
    }

    public static boolean isGPSBasedTrackingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.prefkey_enable_gps_based_track_recording),
                        DEFAULT_PREF_ENABLE_GPS_BASED_TRACK_RECORDING);
    }

    public static final String PREF_RECORDING_TYPE = "pref_recording_type";
    public static final RecordingType DEFAULT_RECORDING_TYPE = RecordingType.OBD_ADAPTER_BASED;

    private static final String PREF_SELECTED_BLUETOOTH_NAME = "pref_selected_bluetooth_name";
    private static final String PREF_SELECTED_BLUETOOTH_ADDRESS = "pref_selected_bluetooth_address";

    public static void setSelectedRecordingType(final Context context, RecordingType recordingType) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_RECORDING_TYPE, recordingType.toString())
                .apply();
    }

    public static Observable<RecordingType> getSelectedRecordingTypeObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getEnum(PREF_RECORDING_TYPE, DEFAULT_RECORDING_TYPE, RecordingType.class)
                .asObservable();
    }

    public static Observable<Pair<String, String>> getSelectedBluetoothAdapterObservable(Context context) {
        return getRxSharedPreferences(context)
                .getString(PREF_SELECTED_BLUETOOTH_ADDRESS).asObservable()
                .map(address -> {
                    String name = getSharedPreferences(context).getString(PREF_SELECTED_BLUETOOTH_NAME, "");
                    return new Pair(name, address);
                });
    }

    public static void setSelectedBluetoothAdapter(Context context, BluetoothDevice device) {
        if (device == null) {
            resetSelectedBluetoothAdapter(context);
        } else {
            getSharedPreferences(context)
                    .edit()
                    .putString(PREF_SELECTED_BLUETOOTH_NAME, device.getName())
                    .putString(PREF_SELECTED_BLUETOOTH_ADDRESS, device.getAddress())
                    .apply();
        }
    }

    public static void resetSelectedBluetoothAdapter(Context context) {
        getSharedPreferences(context)
                .edit()
                .remove(PREF_SELECTED_BLUETOOTH_NAME)
                .remove(PREF_SELECTED_BLUETOOTH_ADDRESS)
                .apply();
    }

    private static RxSharedPreferences getRxSharedPreferences(Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context));
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        Preconditions.checkNotNull(context, "Input context cannot be null.");
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Observable<String> getCampaignProfileObservable(Context context){
        return getRxSharedPreferences(context)
                .getString(s(context, R.string.prefkey_campaign_profile), DEFAULT_CAMPAIGN_PROFILE)
                .asObservable();
    }

    private static final String s(Context context, int id){
        return context.getString(id);
    }

}
