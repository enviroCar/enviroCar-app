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
package org.envirocar.app.application.service;

import java.util.ArrayList;

import org.envirocar.app.activity.SettingsActivity;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;

public class DeviceInRangeService extends Service {

	public static final String DEVICE_FOUND = DeviceInRangeService.class.getName().concat(".DEVICE_FOUND");
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				verifyRemoteDevice(intent);
			}

		}
	};

	@Override
	public void onCreate() {
		registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
	}

	protected void verifyRemoteDevice(Intent intent) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String remoteDevice = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);
		
		if (remoteDevice != null) {
			BluetoothDevice discoveredDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if (remoteDevice.equals(discoveredDevice.getAddress())) {
				initializeConnection(discoveredDevice);
			}
		}
	}

	private void initializeConnection(BluetoothDevice discoveredDevice) {
		if (connectionAlreadyEstablished()) return;
		Intent intent = new Intent(DEVICE_FOUND);
		ArrayList<Parcelable> list = new ArrayList<Parcelable>();
		list.add(discoveredDevice);
		intent.putParcelableArrayListExtra(DEVICE_FOUND, list);
		sendBroadcast(intent);
	}

	private boolean connectionAlreadyEstablished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
