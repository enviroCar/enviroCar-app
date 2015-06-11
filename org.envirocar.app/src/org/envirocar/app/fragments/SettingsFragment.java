package org.envirocar.app.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.envirocar.app.Injector;
import org.envirocar.app.R;
import org.envirocar.app.util.Util;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author dewall
 */
public class SettingsFragment extends PreferenceFragment {
    public static final String BLUETOOTH_KEY = "bluetooth_list";
    public static final String BLUETOOTH_NAME = "bluetooth_name";
    public static final String AUTO_BLUETOOH = "pref_auto_bluetooth";
    public static final String WIFI_UPLOAD = "pref_wifi_upload";
    public static final String ALWAYS_UPLOAD = "pref_always_upload";
    public static final String DISPLAY_STAYS_ACTIV = "pref_display_always_activ";
    public static final String IMPERIAL_UNIT = "pref_imperial_unit";
    public static final String OBFUSCATE_POSITION = "pref_privacy";
    public static final String CAR = "pref_selected_car";
    public static final String CAR_HASH_CODE = "pref_selected_car_hash_code";
    public static final String PERSISTENT_SEEN_ANNOUNCEMENTS = "persistent_seen_announcements";
    public static final String SAMPLING_RATE = "ec_sampling_rate";
    public static final String ENABLE_DEBUG_LOGGING = "pref_enable_debug_logging";

    public static String[] resolveIndividualKeys() {
        // TODO
//        UserManager o = mUserManager.instance();
//        try {
//            Method m = o.getClass().getDeclaredMethod("getUserPreferences", new Class<?>[0]);
//            m.setAccessible(true);
//            SharedPreferences p = (SharedPreferences) m.invoke(o, new Object[0]);
//            m.setAccessible(false);
//            String[] result = new String[0];
//            result = p.getAll().keySet().toArray(result);
//            return result;
//        } catch (Exception e) {
//        }
        return new String[0];
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        initializeBluetoothList();
        ((Injector) getActivity()).injectObjects(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        Preference about = findPreference("about_version");
        about.setSummary(Util.getVersionString(getActivity()));
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.envirocar_org)));
                startActivity(i);
                return true;
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Helper method that cares about the bluetooth list
     */
    private void initializeBluetoothList() {

        // Init Lists

        ArrayList<CharSequence> possibleDevices = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();

        // Get the bluetooth preference if possible

        final ListPreference bluetoothDeviceList = (ListPreference) getPreferenceScreen()
                .findPreference(BLUETOOTH_KEY);

        // Get the default adapter

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();

        // No Bluetooth available...

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            bluetoothDeviceList.setEnabled(false);
            bluetoothDeviceList.setEntries(possibleDevices
                    .toArray(new CharSequence[0]));
            bluetoothDeviceList.setEntryValues(entryValues
                    .toArray(new CharSequence[0]));


            bluetoothDeviceList.setSummary(R.string.pref_bluetooth_disabled);

            return;
        }

        // Prepare getting the devices


        bluetoothDeviceList.setEntries(new CharSequence[1]);
        bluetoothDeviceList.setEntryValues(new CharSequence[1]);

        // Listen for clicks on the list
        bluetoothDeviceList
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        if (bluetoothAdapter == null
                                || !bluetoothAdapter.isEnabled()) {
                            Toast.makeText(getActivity(),
                                    "No Bluetooth support. Is Bluetooth on?",
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        return true;
                    }
                });
        //change summary of preference accordingly
        bluetoothDeviceList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                bluetoothDeviceList.setValue(newValue.toString());
                preference.setSummary(bluetoothDeviceList.getEntry());
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString
                        (BLUETOOTH_NAME, (String) bluetoothDeviceList.getEntry()).commit();
                return false;
            }
        });

        // Get all paired devices...

        Set<BluetoothDevice> availablePairedDevices = bluetoothAdapter
                .getBondedDevices();

        // ...and add them to the list of available paired devices

        if (availablePairedDevices.size() > 0) {
            for (BluetoothDevice device : availablePairedDevices) {
                possibleDevices.add(device.getName() + "\n" + device.getAddress());
                if (device.getAddress().equals(PreferenceManager.getDefaultSharedPreferences
                        (getActivity()).getString(BLUETOOTH_KEY, ""))) {
                    bluetoothDeviceList.setSummary(device.getName() + " " + device.getAddress());
                }
                entryValues.add(device.getAddress());
            }
        }

        bluetoothDeviceList.setEntries(possibleDevices
                .toArray(new CharSequence[0]));
        bluetoothDeviceList.setEntryValues(entryValues
                .toArray(new CharSequence[0]));

    }
}
