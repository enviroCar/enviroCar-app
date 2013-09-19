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
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
import org.envirocar.app.logging.Logger;

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
	
	private static final Logger logger = Logger.getLogger(DeviceInRangeService.class);

	public static final String DEVICE_FOUND = DeviceInRangeService.class.getName().concat(".DEVICE_FOUND");
	public static final String DELAY_EXTRA = DeviceInRangeService.class.getName().concat(".INITIAL_DELAY");
	public static final String STATE_CHANGE = DeviceInRangeService.class.getName().concat(".STATE_CHANGE");
	public static final String TARGET_CONNECTION_TIME = DeviceInRangeService.class.getName().concat(".TARGET_CONNECTION_TIME");
	
	private static final long DISCOVERY_PERIOD = 1000 * 10 * 2;
	
	protected ServiceState backgroundServiceState = ServiceState.SERVICE_STOPPED;

	protected boolean autoConnect;
	
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				verifyRemoteDevice(intent);
			}
			
			else if (action.equals(STATE_CHANGE)) {
				if (!intent.getBooleanExtra(STATE_CHANGE, false)) {
					discoveryEnabled = false;
					stopSelf();	
				}
			}

		}
	};
	

	
	private Runnable discoveryRunnable;
	protected boolean discoveryEnabled = true;
	private Handler discoveryHandler;

	@Override
	public void onCreate() {
		logger.info("onCreate " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(receiver, new IntentFilter(STATE_CHANGE));
		
		discoveryHandler = new Handler();
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		logger.info("onRebind " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		logger.info("onUnbind " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		logger.info("onDestroy " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		unregisterReceiver(receiver);
	}

	protected void verifyRemoteDevice(Intent intent) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String targetDeviceFromSettings = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);
		
		if (targetDeviceFromSettings != null) {
			BluetoothDevice discoveredDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			logger.info("Found Device: "+discoveredDevice.getName() +" / "+discoveredDevice.getAddress());
			if (targetDeviceFromSettings.equals(discoveredDevice.getAddress())) {
				initializeConnection(discoveredDevice);
			}
		}
	}

	private void initializeConnection(BluetoothDevice discoveredDevice) {
		discoveryEnabled = false;
		discoveryHandler.removeCallbacks(discoveryRunnable);
		Intent intent = new Intent(DEVICE_FOUND);
		ArrayList<Parcelable> list = new ArrayList<Parcelable>();
		list.add(discoveredDevice);
		intent.putParcelableArrayListExtra(DEVICE_FOUND, list);
		sendBroadcast(intent);
		stopSelf();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.info("onStartCommand " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		startWithDelay(DISCOVERY_PERIOD);
		return super.onStartCommand(intent, flags, startId);
	}

	protected void startWithDelay(long d) {
		if (backgroundServiceState == ServiceState.SERVICE_STARTED) return;
		
		discoveryEnabled = true;
		
		discoveryRunnable = new Runnable() {
			@Override
			public void run() {
				if (!discoveryEnabled) {
					return;
				}
				
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				if (adapter != null) {
					if (adapter.isDiscovering()) {
						adapter.cancelDiscovery();
					}
					adapter.startDiscovery();
				}
				
				/*
				 * re-schedule ourselves
				 */
				invokeDiscoveryRunnable(DISCOVERY_PERIOD);
			}

		};
		
		invokeDiscoveryRunnable(d);
		Intent intent = new Intent(AbstractBackgroundServiceStateReceiver.SERVICE_STATE);
		intent.putExtra(AbstractBackgroundServiceStateReceiver.SERVICE_STATE, ServiceState.SERVICE_DEVICE_DISCOVERY_PENDING);
		sendBroadcast(intent);
	}

	private void invokeDiscoveryRunnable(long delay) {
		discoveryHandler.postDelayed(discoveryRunnable, delay);
		Intent intent = new Intent(TARGET_CONNECTION_TIME);
		intent.putExtra(TARGET_CONNECTION_TIME, System.currentTimeMillis()+delay);
		sendBroadcast(intent);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		logger.info("onBind " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		/*
		 * we do not need a binder, as we are autonomous
		 */
		return null;
	}

}
