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
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;

/**
 * backgroundService for managing the auto-discovery of the
 * specified OBD-II bluetooth device.
 * 
 * @author matthes rieke
 *
 */
public class DeviceInRangeService extends Service {

	public static final String DEVICE_FOUND = DeviceInRangeService.class.getName().concat(".DEVICE_FOUND");
	public static final String DELAY_EXTRA = DeviceInRangeService.class.getName().concat(".INITIAL_DELAY");
	
	private static final long DISCOVERY_PERIOD = 1000 * 60 * 2;
	public static final int DEFAULT_DELAY_AFTER_STOP = 1000 * 60 * 5;
	
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
	private Runnable discoveryRunnable;
	protected boolean discoveryEnabled = true;
	private int delay;

	@Override
	public void onCreate() {
		registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
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
		discoveryEnabled = false;
		Intent intent = new Intent(DEVICE_FOUND);
		ArrayList<Parcelable> list = new ArrayList<Parcelable>();
		list.add(discoveredDevice);
		intent.putParcelableArrayListExtra(DEVICE_FOUND, list);
		sendBroadcast(intent);
		stopSelf();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/*
		 * the delay as specified in the intent
		 */
		if (intent != null) {
			this.delay = intent.getIntExtra(DELAY_EXTRA, 0);
		}
		
		final Handler discoveryHandler = new Handler();
		
		discoveryRunnable = new Runnable() {
			@Override
			public void run() {
				if (!discoveryEnabled) {
					return;
				}
				
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				if (adapter != null && !adapter.isDiscovering()) {
					adapter.startDiscovery();
				}
				
				/*
				 * re-schedule ourselves
				 */
				discoveryHandler.postDelayed(this, DISCOVERY_PERIOD);
			}
		};
		
		discoveryHandler.postDelayed(discoveryRunnable, DISCOVERY_PERIOD + delay);
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		/*
		 * we do not need a binder, as we are autonomous
		 */
		return null;
	}

}
