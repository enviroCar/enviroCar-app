package org.envirocar.app.view.settings;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.core.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.core.injection.Injector;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class OBDSettingsFragment extends PreferenceFragment {
    private static final Logger LOG = Logger.getLogger(OBDSettingsFragment.class);

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

        // Set the preference resource.
        addPreferencesFromResource(R.xml.preferences_obd);

        // Get the switch preference indicating the enabled bluetooth setting.
        mBluetoothIsActivePreference = (SwitchPreference) getPreferenceScreen()
                .findPreference(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_ENABLER);
        // Get the bluetooth pairing preference if possible
        mBluetoothPairingPreference = getPreferenceScreen()
                .findPreference(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_PAIRING);
        // Get the bluetooth preference if possible
        mBluetoothDeviceListPreference = getPreferenceScreen()
                .findPreference(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_LIST);

        updateBluetoothPreferences(mBluetoothHandler.isBluetoothEnabled());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // SwitchPreference preference change listener, which enables and disables bluetooth.
        mBluetoothIsActivePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isOn = (boolean) newValue;
            if (isOn) {
                mBluetoothHandler.enableBluetooth(getActivity());
                return false;
            } else {
                mBluetoothHandler.disableBluetooth(getActivity());
            }
            return true;
        });

        // Checks wheter bluetooth is on and, if so, starts the intended preference.
        mBluetoothDeviceListPreference.setOnPreferenceClickListener(preference -> {
            if (!mBluetoothHandler.isBluetoothEnabled()) {
                Toast.makeText(getActivity(), "No Bluetooth support. Is Bluetooth on?",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        });

        // Set the color of the background from transparent to white.
        getView().setBackgroundColor(getResources().getColor(R.color.white_cario));
    }

    @Override
    public void onStart() {
        super.onStart();

        // Register this object on the event bus.
        mBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister this object from the event bus.
        mBus.unregister(this);
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOG.debug("onBluetoothStateChangedEvent(): " + event.toString());
        updateBluetoothPreferences(event.isBluetoothEnabled);
    }

    @Subscribe
    public void onBluetoothPairingChangedEvent(BluetoothPairingChangedEvent event) {
        LOG.severe("onBluetoothPairingChangedEvent(): " + event.toString());

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
            mBluetoothDeviceListPreference.setSummary(R.string
                    .pref_bluetooth_select_adapter_summary);

            // If there is already a device that is selected as OBD Adapter, then update the
            // summary of the preference.
            BluetoothDevice selectedBluetoothDevice = mBluetoothHandler
                    .getSelectedBluetoothDevice();
            if (selectedBluetoothDevice != null) {
                mBluetoothDeviceListPreference.setSummary(selectedBluetoothDevice.getName() + " "
                        + selectedBluetoothDevice.getAddress());
            }
        }
    }
}
