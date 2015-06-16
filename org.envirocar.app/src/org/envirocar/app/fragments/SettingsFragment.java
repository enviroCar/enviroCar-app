package org.envirocar.app.fragments;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.Injector;
import org.envirocar.app.R;
import org.envirocar.app.bluetooth.event.BluetoothPairingChangedEvent;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.util.Util;
import org.envirocar.app.view.preferences.PreferencesConstants;

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
    private Preference mBluetoothDeviceListPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject all required dependencies.
        ((Injector) getActivity()).injectObjects(this);

        addPreferencesFromResource(R.xml.preferences);

        // Register this object on the event bus.
        mBus.register(this);
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

        // Get the bluetooth preference if possible
        mBluetoothDeviceListPreference = getPreferenceScreen()
                .findPreference(PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_LIST);
        updateBluetoothList(mBluetoothHandler.isBluetoothEnabled());

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
        updateBluetoothList(event.isBluetoothEnabled);
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
                    .remove(PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_NAME)
                    .remove(PreferencesConstants.PREFERENCE_TAG_BLUETOOTH_ADDRESS).commit();
        }
    }

    /**
     * Helper method that cares about the bluetooth list
     */
    private void updateBluetoothList(boolean isEnabled) {
        // No Bluetooth available...
        if (!isEnabled) {
            mBluetoothDeviceListPreference.setEnabled(false);
            mBluetoothDeviceListPreference.setSummary(
                    R.string.pref_bluetooth_disabled);
        }
        // Bluetooth is available...
        else {
            // Enable the Bluetooth Button.
            mBluetoothDeviceListPreference.setEnabled(true);

            // If there is already a device that is selected as OBD Adapter, then update the
            // summary of the preference.
            BluetoothDevice selectedBluetoothDevice = mBluetoothHandler.getSelectedBluetoothDevice();
            if (selectedBluetoothDevice != null) {
                mBluetoothDeviceListPreference.setSummary(selectedBluetoothDevice.getName() + " "
                        + selectedBluetoothDevice.getAddress());
            } else {

            }
        }
    }
}
