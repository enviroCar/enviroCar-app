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
package org.envirocar.app.handler;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.common.base.Preconditions;

import org.envirocar.app.recording.RecordingType;

import io.reactivex.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class ApplicationSettings {

    // Default values
    public static final boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;
    public static final boolean DEFAULT_DISPLAY_STAYS_ACTIVE = false;
    public static final boolean DEFAULT_TEXT_TO_SPEECH = false;
    public static final boolean DEFAULT_BLUETOOTH_SERVICE_AUTOSTART = true;
    public static final boolean DEFAULT_PREF_ENABLE_GPS_BASED_TRACK_RECORDING = false;
    public static final boolean DEFAULT_AUTOMATIC_UPLOAD_OF_TRACKS = false;
    public static final int DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL = 60;
    public static final int DEFAULT_TRACK_TRIM_DURATION = 110;
    public static final boolean DEFAULT_DEBUG_LOGGING = false;

    // General Settings
    public static final String PREF_AUTOMATIC_UPLOAD_OF_TRACKS = "pref_automatic_upload_tracks";
    public static final String PREF_DISPLAY_STAYS_ACTIVE = "pref_display_stays_active";
    public static final String PREF_USE_IMPERIAL_UNITS = "pref_use_imperial_unit";
    public static final String PREF_OBFUSCATE_TRACK = "pref_obfuscate_track";
    public static final String PREF_TEXT_TO_SPEECH = "pref_text_to_speech";

    // Auto-recording Settings
    public static final String PREF_BACKGROUND_PROCESS = "pref_background_progress";
    public static final String PREF_AUTOMATIC_RECORDING = "pref_automatic_recording";
    public static final String PREF_SEARCH_INTERVAL = "pref_search_interval";

    // Optional Settings
    public static final String PREF_SAMPLING_RATE = "pref_sampling_rate";
    public static final String PREF_DEBUG_LOGGING = "pref_debug_logging";
    public static final String PREF_DIESEL_ESTIMATION = "pref_diesel_estimation";
    public static final String PREF_TRACK_TRIM_DURATION = "pref_track_trim_duration";
    public static final String PREF_GPS_BASED_TRACKING = "pref_gps_based_tracking";

    // General Settings settings
    public static Observable<Boolean> getAutomaticUploadOfTracksObservable(Context context){
        return getRxSharedPreferences(context)
                .getBoolean(PREF_AUTOMATIC_UPLOAD_OF_TRACKS, DEFAULT_AUTOMATIC_UPLOAD_OF_TRACKS)
                .asObservable();
    }

    public static boolean isDisplayStaysActive(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_DISPLAY_STAYS_ACTIVE, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getDisplayStaysActiveObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_DISPLAY_STAYS_ACTIVE, DEFAULT_DISPLAY_STAYS_ACTIVE)
                .asObservable();
    }

    public static boolean isObfuscationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_OBFUSCATE_TRACK, false);
    }

    public static Observable<Boolean> getObfuscationObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_OBFUSCATE_TRACK, false)
                .asObservable();
    }

    public static boolean isTextToSpeechEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH);
    }

    public static Observable<Boolean> getTextToSpeechObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_TEXT_TO_SPEECH, DEFAULT_TEXT_TO_SPEECH)
                .asObservable();
    }

    // Auto-Recording settings
    public static Observable<Boolean> getBackgroundHandlerEnabledObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getBoolean(PREF_BACKGROUND_PROCESS, DEFAULT_BLUETOOTH_SERVICE_AUTOSTART)
                .asObservable();
    }

    public static boolean isAutorecordingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_AUTOMATIC_RECORDING, DEFAULT_BLUETOOTH_AUTOCONNECT);
    }

    public static Observable<Boolean> getAutoconnectObservable(final Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_AUTOMATIC_RECORDING, DEFAULT_BLUETOOTH_AUTOCONNECT)
                .asObservable();
    }

    public static int getDiscoveryInterval(Context context) {
        return getSharedPreferences(context)
                .getInt(PREF_SEARCH_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL);
    }

    public static Observable<Integer> getDiscoveryIntervalObservable(Context context) {
        return getRxSharedPreferences(context)
                .getInteger(PREF_SEARCH_INTERVAL, DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL)
                .asObservable();
    }

    public static void setDiscoveryInterval(Context context, int discoveryInterval) {
        getSharedPreferences(context)
                .edit()
                .putInt(PREF_SEARCH_INTERVAL, discoveryInterval)
                .apply();
    }

    // Optional Settings
    public static Long getSamplingRate(Context context) {
        return Long.parseLong(getSharedPreferences(context).getString(PREF_SAMPLING_RATE, "5"));
    }

    public static Observable<Long> getRxSharedSamplingRate(Context context) {
        return getRxSharedPreferences(context)
                .getString(PREF_SAMPLING_RATE, "5")
                .asObservable().map(s -> Long.parseLong(s));
    }

    public static Observable<Boolean> getDebugLoggingObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_DEBUG_LOGGING, DEFAULT_DEBUG_LOGGING)
                .asObservable();
    }

    public static boolean isDieselConsumptionEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_DIESEL_ESTIMATION, false);
    }

    public static Observable<Boolean> getDieselConsumptionObservable(Context context) {
        return getRxSharedPreferences(context)
                .getBoolean(PREF_DIESEL_ESTIMATION, false)
                .asObservable();
    }

    public static Observable<Integer> getTrackTrimDurationObservable(final Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context))
                .getInteger(PREF_TRACK_TRIM_DURATION, DEFAULT_TRACK_TRIM_DURATION)
                .asObservable();
    }

    public static void setTrackTrimDurationObservable(final Context context, int trackTrimDuration) {
        getSharedPreferences(context).edit()
                .putInt(PREF_TRACK_TRIM_DURATION, trackTrimDuration)
                .apply();
    }

    public static boolean isGPSBasedTrackingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_GPS_BASED_TRACKING, DEFAULT_PREF_ENABLE_GPS_BASED_TRACK_RECORDING);
    }

    public static final String PREF_RECORDING_TYPE = "pref_recording_type";
    public static final String PREF_PREV_VIEW_TYPE_GENERAL_RECORDING_SCREEN = "2202";
    public static final String PREF_PREV_VIEW_TYPE_METER_RECORDING_SCREEN = "4u32848";
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
        getSharedPreferences(context)
                .edit()
                .putString(PREF_SELECTED_BLUETOOTH_NAME, device.getName())
                .putString(PREF_SELECTED_BLUETOOTH_ADDRESS, device.getAddress())
                .apply();
    }


//    public static final String PREF_BLUETOOTH_SERVICE_AUTOSTART = "pref_bluetooth_service_autostart";
//    public static final String PREF_BLUETOOTH_AUTOCONNECT = "pref_bluetooth_autoconnect";
//    public static final String PREF_BLUETOOTH_DISCOVERY_INTERVAL = "pref_bluetooth_discovery_interval";
//
//
//    public static final String PREF_TRACK_CUT_DURATION = "pref_track_cut_duration";
//
//
//    public static final String PREF_ENABLE_DIESE_CONSUMPTION = "pref_enable_diesel_consumption";
//
//    public static final String PREF_TEXT_TO_SPEECH = "pref_text_to_speech";
//
//
//    public static final RecordingType DEFAULT_RECORDING_TYPE = RecordingType.OBD_ADAPTER_BASED;
//
//
//    public static final String DISPLAY_STAYS_ACTIV = "pref_display_always_activ";
//    public static final String IMPERIAL_UNIT = "pref_imperial_unit";
//    public static final String PREF_OBFUSCATE_POSITION = "pref_privacy";
//    public static final String PERSISTENT_SEEN_ANNOUNCEMENTS = "persistent_seen_announcements";
//    public static final String SAMPLING_RATE = "ec_sampling_rate";
//    public static final String ENABLE_DEBUG_LOGGING = "pref_enable_debug_logging";
//
////    public static final String PREF_LOCAL_TRACK_COUNT = "pref_local_track_count";
////    public static final String PREF_UPLOADED_TRACK_COUNT = "pref_uploaded_track_count";
////    public static final String PREF_GLOBAL_TRACK_COUNT = "pref_global_track_count";
////    public static final String PREF_TOTAL_DIST_TRAVELLED = "pref_total_dist_travelled_3";
////    public static final String PREF_TOTAL_TIME = "pref_total_time";
//
//    public static final String PREF_PREV_REC_TYPE = "pref_prev_rec_type";
//
//    public static final String PREF_PREV_VIEW_TYPE_GENERAL_RECORDING_SCREEN = "pref_prev_view_type_general_recording_type";
//    public static final String PREF_PREV_VIEW_TYPE_METER_RECORDING_SCREEN = "pref_prev_view_type_meter_recording_type";
//
//    public static final String PREF_RECORDING_TYPE = "pref_recording_type";
//
//    public static final String PREF_ENABLE_GPS_BASED_TRACK_RCORDING = "pref_enable_gps_based_track_recording";


//    public static long getTrackTrimDuration(Context context) {
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getInt(PREF_TRACK_CUT_DURATION, DEFAULT_TRACK_TRIM_DURATION);
//    }


//    public static int getPreviousViewTypeGeneralRecordingScreen(Context context) {
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getInt(PREF_PREV_VIEW_TYPE_GENERAL_RECORDING_SCREEN, 1);
//    }
//
//    public static int getPreviousViewTypeGeneralForGPSRecordingScreen(Context context) {
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getInt(PREF_PREV_VIEW_TYPE_GENERAL_RECORDING_SCREEN, 2);
//    }


//    public static void resetTrackCounts(Context context) {
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_LOCAL_TRACK_COUNT, 0)
//                .putInt(PREF_UPLOADED_TRACK_COUNT, 0).putInt(PREF_GLOBAL_TRACK_COUNT, 0).putFloat(PREF_TOTAL_DIST_TRAVELLED, 0.0f).putString(PREF_TOTAL_TIME, "No Tracks").apply();
//    }

//    public static int getPreviouslySelectedRecordingType(Context context) {
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getInt(PREF_PREV_REC_TYPE, 1);
//    }
//
//    public static Observable<Integer> getPreviouslySelectedRecordingTypeObservable(final Context context) {
//        return RxSharedPreferences.create(getSharedPreferences(context))
//                .getInteger(PREF_PREV_REC_TYPE, 1)
//                .asObservable();
//    }


//
//    public static void setPreviouslySelectedRecordingType(Context context, int index) {
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_PREV_REC_TYPE, index).apply();
//    }


//    public static boolean isBackgroundHandlerEnabled(Context context) {
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getBoolean(PREF_BLUETOOTH_SERVICE_AUTOSTART, DEFAULT_BLUETOOTH_SERVICE_AUTOSTART);
//    }


    private static RxSharedPreferences getRxSharedPreferences(Context context) {
        return RxSharedPreferences.create(getSharedPreferences(context));
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        Preconditions.checkNotNull(context, "Input context cannot be null.");
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
