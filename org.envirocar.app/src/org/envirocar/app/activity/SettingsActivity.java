/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.activity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.envirocar.app.R;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.util.ConsumptionVolumeUnit;
import org.envirocar.app.util.DistanceUnit;
import org.envirocar.app.util.SpeedUnit;
import org.envirocar.app.util.Util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * Settings class that deals with bluetooth select, autoconnect, auto upload and
 * wifi-upload
 * 
 * @author jakob
 * 
 */
public class SettingsActivity extends SherlockPreferenceActivity {

	public static final String BLUETOOTH_KEY = "bluetooth_list";
	public static final String BLUETOOTH_NAME = "bluetooth_name";
	public static final String AUTO_BLUETOOH = "pref_auto_bluetooth";
	public static final String WIFI_UPLOAD = "pref_wifi_upload";
	public static final String ALWAYS_UPLOAD = "pref_always_upload";
	public static final String DISPLAY_STAYS_ACTIV = "pref_display_always_activ";
	public static final String OBFUSCATE_POSITION = "pref_privacy";
	public static final String CAR = "pref_selected_car";
	public static final String CAR_HASH_CODE = "pref_selected_car_hash_code";
	public static final String PERSISTENT_SEEN_ANNOUNCEMENTS = "persistent_seen_announcements";

	public static final String SPEED_UNITS_LIST_KEY = "pref_speed_units_list";
	public static final String FUEL_VOLUME_UNITS_LIST_KEY = "pref_fuel_volume_units_list";
	public static final String CONSUMPTION_UNITS_LIST_KEY = "pref_consumption_units_list";
	public static final String DISTANCE_UNITS_LIST_KEY = "pref_distance_units_list";
	
	private Preference about;
	
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
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(BLUETOOTH_NAME, (String) bluetoothDeviceList.getEntry()).commit();
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
				if(device.getAddress().equals(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(BLUETOOTH_KEY, ""))){
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

	private void initializeUnitLists(){
		
		java.util.Map<CharSequence, CharSequence> speedUnits = new HashMap<CharSequence, CharSequence>(2);
		
		Resources resources = getApplicationContext().getResources(); 
		
		speedUnits.put(resources.getString(R.string.miles_per_hour), SpeedUnit.MILES_PER_HOUR.toString());
		speedUnits.put(resources.getString(R.string.kilometers_per_hour), SpeedUnit.KILOMETER_PER_HOUR.toString());
		
		setUpListValues(SPEED_UNITS_LIST_KEY, speedUnits);
		
		java.util.Map<CharSequence, CharSequence> distanceUnits = new HashMap<CharSequence, CharSequence>(2);
				
		distanceUnits.put(resources.getString(R.string.miles), DistanceUnit.MILES.toString());
		distanceUnits.put(resources.getString(R.string.kilometers), DistanceUnit.KILOMETER.toString());
		
		setUpListValues(DISTANCE_UNITS_LIST_KEY, distanceUnits);
	
		java.util.Map<CharSequence, CharSequence> fuelVolumeUnits = new HashMap<CharSequence, CharSequence>(3);
		
		fuelVolumeUnits.put(resources.getString(R.string.us_liquid_gallon), ConsumptionVolumeUnit.US_GALLON.toString());
		fuelVolumeUnits.put(resources.getString(R.string.imperial_gallon), ConsumptionVolumeUnit.IMPERIAL_GALLON.toString());
		fuelVolumeUnits.put(resources.getString(R.string.liter), ConsumptionVolumeUnit.LITER.toString());
		
		setUpListValues(FUEL_VOLUME_UNITS_LIST_KEY, fuelVolumeUnits);
		
		ArrayList<CharSequence> consumptionUnits = new ArrayList<CharSequence>(6);
		
		consumptionUnits.add(resources.getString(R.string.miles_per_us_gallon));
		consumptionUnits.add(resources.getString(R.string.miles_per_imperial_gallon));
		consumptionUnits.add(resources.getString(R.string.liters_per_100_km));
		consumptionUnits.add(resources.getString(R.string.kilometer_per_liter));
		consumptionUnits.add(resources.getString(R.string.us_gallons_per_100_miles));
		consumptionUnits.add(resources.getString(R.string.imperial_gallons_per_100_miles));
		
		setUpListValues(CONSUMPTION_UNITS_LIST_KEY, consumptionUnits);		
		
	}
	
	private void setUpListValues(String listKey, Collection<CharSequence> entries, Collection<CharSequence> values){
		
		ListPreference list = (ListPreference) getPreferenceScreen()
				.findPreference(listKey);
		
		list.setEntries(entries.toArray(new CharSequence[]{}));
		list.setEntryValues(values.toArray(new CharSequence[]{}));
		
		String preferredValue = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(listKey, null);
		
		if(preferredValue != null){
			list.setValue(preferredValue);
		}
		
	}
	
	private void setUpListValues(String listKey, Collection<CharSequence> entriesAndValues){
		setUpListValues(listKey, entriesAndValues, entriesAndValues);
		
	}
	
	private void setUpListValues(String listKey, java.util.Map<CharSequence, CharSequence> entriesAndValues){
		setUpListValues(listKey, entriesAndValues.keySet(), entriesAndValues.values());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		initializeBluetoothList();
		initializeUnitLists();
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		this.getSupportActionBar().setHomeButtonEnabled(false);

		about = findPreference("about_version");
		about.setSummary(Util.getVersionString(getApplicationContext()));
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.envirocar_org)));
				startActivity(i);
				return true;
			}
		});		

	}

	/**
	 * Called when activity leaves the foreground.
	 */
	protected void onStop() {
	    super.onStop();
	    finish();
	}

	public static String[] resolveIndividualKeys() {
		UserManager o = UserManager.instance();
		try {
			Method m = o.getClass().getDeclaredMethod("getUserPreferences", new Class<?>[0]);
			m.setAccessible(true);
			SharedPreferences p = (SharedPreferences) m.invoke(o, new Object[0]);
			m.setAccessible(false);
			String[] result = new String[0];
			result = p.getAll().keySet().toArray(result);
			return result;
		} catch (Exception e) {
		}
		return new String[0];
	}
}
