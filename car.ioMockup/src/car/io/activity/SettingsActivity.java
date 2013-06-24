package car.io.activity;

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
import android.widget.Toast;
import car.io.R;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingsActivity extends SherlockPreferenceActivity {

	public static final String BLUETOOTH_KEY = "bluetooth_list";
	public static final String BLUETOOTH_NAME = "bluetooth_name";
	public static final String AUTOCONNECT = "pref_auto_connect";
	public static final String AUTO_BLUETOOH = "pref_auto_bluetooth";
	public static final String WIFI_UPLOAD = "pref_wifi_upload";
	public static final String ALWAYS_UPLOAD = "pref_always_upload";
	
	/**
	 * Helper method that cares about the bluetooth list
	 */

	@SuppressWarnings("deprecation")
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
									Toast.LENGTH_SHORT).show();
							return false;
						}
						return true;
					}
				});
		//change summary of preference accordingly
		bluetoothDeviceList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				bluetoothDeviceList.setValue(newValue.toString());
				preference.setSummary(bluetoothDeviceList.getEntry());
				getPreferenceManager().getDefaultSharedPreferences(thisSettingsActivity).edit().putString(BLUETOOTH_NAME, (String) bluetoothDeviceList.getEntry()).commit();
				return false;
			}
		});

		// Get all paired devices...

		Set<BluetoothDevice> availablePairedDevices = bluetoothAdapter
				.getBondedDevices();

		// ...and add them to the list of available paired devices

		if (availablePairedDevices.size() > 0) {
			for (BluetoothDevice device : availablePairedDevices) {
				possibleDevices.add(device.getName() + "\n"+ device.getAddress());
				if(device.getAddress().equals(getPreferenceManager().getDefaultSharedPreferences(thisSettingsActivity).getString(BLUETOOTH_KEY, ""))){
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

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		initializeBluetoothList();
	}

	/**
	 * Called when activity leaves the foreground.
	 */
	protected void onStop() {
	    super.onStop();
	    finish();
	}
}
