package org.envirocar.app.view;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.app.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.util.Util;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class SettingsFragment extends PreferenceFragment {
    private static final Logger LOGGER = Logger.getLogger(SettingsFragment.class);

    // Injected Variables
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // Preferences.
    private SwitchPreference mBluetoothIsActivePreference;
    private Preference mBluetoothPairingPreference;
    private Preference mBluetoothDeviceListPreference;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject all required dependencies.
        ((Injector) getActivity()).injectObjects(this);

        addPreferencesFromResource(R.xml.preferences);

        // Register this object on the event bus.
        mBus.register(this);

        // Get the switch preference indicating the enabled bluetooth setting.
        mBluetoothIsActivePreference = (SwitchPreference) getPreferenceScreen().findPreference
                (PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_ENABLER);
        // Get the bluetooth pairing preference if possible
        mBluetoothPairingPreference = getPreferenceScreen().findPreference(PreferenceConstants
                .PREFERENCE_TAG_BLUETOOTH_PAIRING);
        // Get the bluetooth preference if possible
        mBluetoothDeviceListPreference = getPreferenceScreen()
                .findPreference(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_LIST);

        updateBluetoothPreferences(mBluetoothHandler.isBluetoothEnabled());
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

        mBluetoothIsActivePreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isOn = (boolean) newValue;
                        if (isOn) {
                            mBluetoothHandler.enableBluetooth(getActivity());
                            return false;
                        } else {
                            mBluetoothHandler.disableBluetooth(getActivity());
                        }
                        return true;
                    }
                });
//        mBluetoothDeviceListPreference.

        // Listen for clicks on the list
        mBluetoothDeviceListPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (!mBluetoothHandler.isBluetoothEnabled()) {
                            Toast.makeText(getActivity(),
                                    "No Bluetooth support. Is Bluetooth on?",
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        return true;
                    }
                });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.debug("onBluetoothStateChangedEvent(): " + event.toString());
        updateBluetoothPreferences(event.isBluetoothEnabled);
    }

    @Subscribe
    public void onBluetoothPairingChangedEvent(BluetoothPairingChangedEvent event) {
        LOGGER.severe("onBluetoothPairingChangedEvent(): " + event.toString());

        // Remove all
        if (!event.mIsPaired && mBluetoothHandler.getSelectedBluetoothDevice() == null) {
            mBluetoothDeviceListPreference.setSummary(
                    R.string.pref_bluetooth_select_adapter_summary);

            // remove the shared preference entries for the bluetooth selection tag.
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .remove(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_NAME)
                    .remove(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_ADDRESS).commit();
        }
    }

    /**
     * Helper method that cares about the bluetooth lists
     */
    private void updateBluetoothPreferences(boolean isEnabled) {

        // No Bluetooth available...
        if (!isEnabled) {

            // Set the switch for enabling bluetooth stuff accordingly.
            mBluetoothIsActivePreference.setChecked(isEnabled);
            mBluetoothIsActivePreference.setTitle(R.string.pref_bluetooth_switch_isdisabled);

            // Update the pairing list preference
            mBluetoothPairingPreference.setEnabled(false);
            mBluetoothPairingPreference.setSummary(R.string.pref_bluetooth_disabled);

            // Update the BluetoothDeviceList
            mBluetoothDeviceListPreference.setEnabled(false);
            mBluetoothDeviceListPreference.setSummary(R.string.pref_bluetooth_disabled);
        }
        // Bluetooth is available...
        else {

            // Set the switch for enabling bluetooth stuff accordingly.
            mBluetoothIsActivePreference.setChecked(isEnabled);
            mBluetoothIsActivePreference.setTitle(R.string.pref_bluetooth_switch_isenabled);

            // Update the pairing list preference
            mBluetoothPairingPreference.setEnabled(true);
            mBluetoothPairingPreference.setSummary(R.string.pref_bluetooth_pairing_summery);

            // Enable the Bluetooth Button.
            mBluetoothDeviceListPreference.setEnabled(true);
            mBluetoothDeviceListPreference.setSummary(R.string.pref_bluetooth_select_adapter_summary);

            // If there is already a device that is selected as OBD Adapter, then update the
            // summary of the preference.
            BluetoothDevice selectedBluetoothDevice = mBluetoothHandler.getSelectedBluetoothDevice();
            if (selectedBluetoothDevice != null) {
                mBluetoothDeviceListPreference.setSummary(selectedBluetoothDevice.getName() + " "
                        + selectedBluetoothDevice.getAddress());
            }
        }
    }
}
