package com.ifgi.obd2.activity;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.ifgi.obd2.R;

/**
 * Settings activity that can be called in the main activity. The user has to
 * select car type, fuel type and bluetooth device here before the measurement
 * can begin
 * 
 * @author jakob
 * 
 */

public class Settings extends PreferenceActivity implements
		OnPreferenceChangeListener {

	public static final String BLUETOOTH_KEY = "bluetooth_list";
	public static final String FUEL_TYPE_KEY = "fueltype_list_preference";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get the Preferences from the preferences.xml

		addPreferencesFromResource(R.xml.preferences);

		initializeBluetoothList();
	}

	/**
	 * Helper method that cares about the bluetooth list
	 */

	private void initializeBluetoothList() {

		// Init Lists

		ArrayList<CharSequence> possibleDevices = new ArrayList<CharSequence>();
		ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();

		// Get the bluetooth preference if possible

		ListPreference bluetoothDeviceList = (ListPreference) getPreferenceScreen()
				.findPreference(BLUETOOTH_KEY);

		// Get the default adapter

		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		// No Bluetooth available...

		if (bluetoothAdapter == null) {
			bluetoothDeviceList.setEntries(possibleDevices
					.toArray(new CharSequence[0]));
			bluetoothDeviceList.setEntryValues(entryValues
					.toArray(new CharSequence[0]));

			Toast.makeText(this, "No Bluetooth available!", Toast.LENGTH_SHORT);

			return;
		}

		// Prepare getting the devices

		final Activity thisSettingsActivity = this;
		bluetoothDeviceList.setEntries(new CharSequence[1]);
		bluetoothDeviceList.setEntryValues(new CharSequence[1]);

		// Listen for clicks on the list

		bluetoothDeviceList
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {

						if (bluetoothAdapter == null
								|| !bluetoothAdapter.isEnabled()) {
							Toast.makeText(thisSettingsActivity,
									"No Bluetooth support. Is Bluetooth on?",
									Toast.LENGTH_SHORT);
							return false;
						}
						return true;
					}
				});

		// Get all paired devices...

		Set<BluetoothDevice> availablePairedDevices = bluetoothAdapter
				.getBondedDevices();

		// ...and add them to the list of available paired devices

		if (availablePairedDevices.size() > 0) {
			for (BluetoothDevice device : availablePairedDevices) {
				possibleDevices.add(device.getName() + "\n"
						+ device.getAddress());
				entryValues.add(device.getAddress());
			}
		}

		bluetoothDeviceList.setEntries(possibleDevices
				.toArray(new CharSequence[0]));
		bluetoothDeviceList.setEntryValues(entryValues
				.toArray(new CharSequence[0]));
	}

	/**
	 * Not needed
	 */
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}

}