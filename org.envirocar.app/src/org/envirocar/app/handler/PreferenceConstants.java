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
