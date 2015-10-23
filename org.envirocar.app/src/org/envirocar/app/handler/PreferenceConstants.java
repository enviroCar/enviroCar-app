package org.envirocar.app.handler;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface PreferenceConstants {
    String PREF_EMPTY = "";
    String PREF_BLUETOOTH_LIST = "bluetooth_list";
    String PREF_BLUETOOTH_NAME = "bluetooth_name";
    String PREF_BLUETOOTH_ADDRESS = "bluetooth_address";
    String PREF_BLUETOOTH_ENABLER = "bluetooth_enabler";
    String PREF_BLUETOOTH_PAIRING = "bluetooth_pairing";

    String PREF_BLUETOOTH_SERVICE_AUTOSTART = "pref_bluetooth_service_autostart";
    String PREF_BLUETOOTH_AUTOCONNECT = "pref_bluetooth_autoconnect";
    String PREF_BLUETOOTH_DISCOVERY_INTERVAL = "pref_bluetooth_discovery_interval";

    String PREF_TEXT_TO_SPEECH = "pref_text_to_speech";

    int DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL = 60;
    boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;


    String PREFERENCE_TAG_CAR = "pref_selected_car";
    String CAR_HASH_CODE = "pref_selected_car_hash_code";
    String PREFERENCE_TAG_CARS = "pref_cars";

    String AUTO_BLUETOOH = "pref_auto_bluetooth";
    String WIFI_UPLOAD = "pref_wifi_upload";
    String ALWAYS_UPLOAD = "pref_always_upload";
    String DISPLAY_STAYS_ACTIV = "pref_display_always_activ";
    String IMPERIAL_UNIT = "pref_imperial_unit";
    String OBFUSCATE_POSITION = "pref_privacy";
    String PERSISTENT_SEEN_ANNOUNCEMENTS = "persistent_seen_announcements";
    String SAMPLING_RATE = "ec_sampling_rate";
    String ENABLE_DEBUG_LOGGING = "pref_enable_debug_logging";
}
