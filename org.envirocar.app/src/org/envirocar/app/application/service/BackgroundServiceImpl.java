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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.TroubleshootingActivity;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.application.Listener;
import org.envirocar.app.application.LocationUpdateListener;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
import org.envirocar.app.bluetooth.BluetoothConnection;
import org.envirocar.app.bluetooth.BluetoothSocketWrapper;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.ConnectionListener;
import org.envirocar.app.protocol.OBDCommandLooper;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import static org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.*;

/**
 * Service for connection to Bluetooth device and running commands. Imported
 * from Android OBD Reader project in some parts.
 * 
 * @author jakob
 * 
 */
public class BackgroundServiceImpl extends Service implements BackgroundService {


	private static final Logger logger = Logger.getLogger(BackgroundServiceImpl.class);
	
	public static final String CONNECTION_PERMANENTLY_FAILED_INTENT =
			BackgroundServiceImpl.class.getName()+".CONNECTION_PERMANENTLY_FAILED";
	
	protected static final long CONNECTION_CHECK_INTERVAL = 1000 * 5;
	// Properties

	private static final String SERVICE_STATE_REQUEST = BackgroundServiceImpl.class.getName()+
			".SERVICE_STATE_REQUEST";

	private Listener commandListener;
	private final Binder binder = new LocalBinder();

	private OBDCommandLooper commandLooper;

	private BluetoothConnection bluetoothConnection;

	protected ServiceState state;


	@Override
	public IBinder onBind(Intent intent) {
		logger.info("onBind " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		return binder;
	}

	@Override
	public void onCreate() {
		logger.info("onCreate " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		
		BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				sendStateBroadcast();
			}
		};
		
		registerReceiver(stateRequestReceiver, new IntentFilter(SERVICE_STATE_REQUEST));
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
		stopBackgroundService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.info("onStartCommand " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
		startBackgroundService();
		return START_STICKY;
	}

	/**
	 * Starts the background service (bluetooth connction). Then calls methods
	 * to start sending the obd commands for initialization.
	 */
	private void startBackgroundService() {
		logger.info("startBackgroundService called");
		LocationUpdateListener.startLocating((LocationManager) getSystemService(Context.LOCATION_SERVICE));
		
		try {
			startConnection();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	/**
	 * Method that stops the service, removes everything from the waiting list
	 */
	private void stopBackgroundService() {
		logger.info("stopBackgroundService called");
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (BackgroundServiceImpl.this.bluetoothConnection != null) {
					BackgroundServiceImpl.this.bluetoothConnection.cancelConnection();
				}
				
				if (BackgroundServiceImpl.this.commandLooper != null) {
					BackgroundServiceImpl.this.commandLooper.stopLooper();
				}
				
				state = ServiceState.SERVICE_STOPPED;
				sendStateBroadcast();
				
				LocationUpdateListener.stopLocating((LocationManager) getSystemService(Context.LOCATION_SERVICE));
				
				if (BackgroundServiceImpl.this.commandListener != null) {
					BackgroundServiceImpl.this.commandListener.shutdown();
				}
			}
		}).start();
	}
	
	private void sendStateBroadcast() {
		Intent intent = new Intent(SERVICE_STATE);
		intent.putExtra(SERVICE_STATE, state);
		sendBroadcast(intent);
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws IOException
	 */
	private void startConnection() throws IOException {
		logger.info("startConnection called");
		// Connect to bluetooth device
		// Init bluetooth
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String remoteDevice = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);
		// Stop if device is not available
		
		if (remoteDevice == null || "".equals(remoteDevice)) {
			return;
		}
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(remoteDevice);

		bluetoothConnection = new BluetoothConnection(bluetoothDevice, true, this, getApplicationContext());
		
		state = ServiceState.SERVICE_STARTING;
		sendStateBroadcast();
	}
	
	
	public void deviceDisconnected() {
		logger.info("Bluetooth device disconnected.");
		stopBackgroundService();
	}
	
	/**
	 * method gets called when the bluetooth device connection
	 * has been established. 
	 */
	public void deviceConnected(BluetoothSocketWrapper bluetoothSocket) {
		logger.info("Bluetooth device connected.");
		
		InputStream in;
		OutputStream out;
		try {
			in = bluetoothSocket.getInputStream();
			out = bluetoothSocket.getOutputStream();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			deviceDisconnected();
			return;
		}
		
		initializeCommandLooper(in, out, bluetoothSocket.getRemoteDeviceName());
	}

	protected void initializeCommandLooper(InputStream in, OutputStream out, String deviceName) {
		commandListener = new CommandListener(CarManager.instance().getCar());
		this.commandLooper = new OBDCommandLooper(
				in, out, deviceName,
				this.commandListener, new ConnectionListener() {
					@Override
					public void onConnectionVerified() {
						state = ServiceState.SERVICE_STARTED;
						BackgroundServiceImpl.this.sendStateBroadcast();
					}
					
					@Override
					public void onConnectionException(IOException e) {
						logger.warn("onConnectionException", e);
						BackgroundServiceImpl.this.deviceDisconnected();
					}

					@Override
					public void onAllAdaptersFailed() {
						BackgroundServiceImpl.this.onAllAdaptersFailed();
					}

					@Override
					public void onStatusUpdate(String message) {
						Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
					}
				});
		this.commandLooper.start();
	}

	public void onAllAdaptersFailed() {
		logger.info("all adapters failed!");
		stopBackgroundService();
		sendBroadcast(new Intent(CONNECTION_PERMANENTLY_FAILED_INTENT));		
	}
	
	public void openTroubleshootingActivity(int type) {
		Intent intent = new Intent(getApplicationContext(), TroubleshootingActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt(TroubleshootingActivity.ERROR_TYPE, type);
		intent.putExtras(bundle);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		getApplication().startActivity(intent);
	}
	
	/**
	 * Binder imported directly from Android OBD Project. Runs the waiting list
	 * when jobs are added to it
	 * 
	 * @author jakob
	 * 
	 */
	private class LocalBinder extends Binder implements BackgroundServiceInteractor {
	
		@Override
		public void initializeConnection() {
//			startBackgroundService();
		}
		
		@Override
		public void shutdownConnection() {
			logger.info("stopping service!");
			stopBackgroundService();
		}

		@Override
		public void allAdaptersFailed() {
			logger.info("all adapters failed!");
			onAllAdaptersFailed();
		}
	}

	public static void requestServiceStateBroadcast(Context ctx) {
		ctx.sendBroadcast(new Intent(SERVICE_STATE_REQUEST));
	}
	
}