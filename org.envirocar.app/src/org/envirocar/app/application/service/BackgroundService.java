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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.TroubleshootingActivity;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.application.Listener;
import org.envirocar.app.application.LocationUpdateListener;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.ConnectionListener;
import org.envirocar.app.protocol.OBDCommandLooper;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import static org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.*;

/**
 * Service for connection to Bluetooth device and running commands. Imported
 * from Android OBD Reader project in some parts.
 * 
 * @author jakob
 * 
 */
public class BackgroundService extends Service {


	private static final Logger logger = Logger.getLogger(BackgroundService.class);
	
	public static final String CONNECTION_VERIFIED_INTENT = BackgroundService.class.getName()+".CONNECTION_VERIFIED";
	public static final String CONNECTION_PERMANENTLY_FAILED_INTENT =
			BackgroundServiceInteractor.class.getName()+".CONNECTION_PERMANENTLY_FAILED";
	
	protected static final long CONNECTION_CHECK_INTERVAL = 1000 * 5;
	// Properties

	// Bluetooth devices and connection items

	private BluetoothSocket bluetoothSocket;
	private static final UUID EMBEDDED_BOARD_SPP = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private Listener commandListener;
	private final Binder binder = new LocalBinder();

	private OBDCommandLooper commandLooper;


	@Override
	public IBinder onBind(Intent intent) {
		logger.info("onBind " + getClass().getName() +" "+this.hashCode());
		return binder;
	}

	@Override
	public void onCreate() {
		logger.info("onCreate " + getClass().getName() +" "+this.hashCode());
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		logger.info("onRebind " + getClass().getName() +" "+this.hashCode());
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		logger.info("onUnbind " + getClass().getName() +" "+this.hashCode());
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		logger.info("onDestroy " + getClass().getName() +" "+this.hashCode());
		stopBackgroundService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.info("onStartCommand " + getClass().getName() +" "+this.hashCode());
		startBackgroundService();
		return START_STICKY;
	}

	/**
	 * Starts the background service (bluetooth connction). Then calls methods
	 * to start sending the obd commands for initialization.
	 */
	private void startBackgroundService() {
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
		if (this.commandLooper != null) {
			this.commandLooper.stopLooper();
		}
		
		sendStateBroadcast(SERVICE_STOPPED);
		
		if (bluetoothSocket != null) {
			try {
				shutdownSocket();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}

		LocationUpdateListener.stopLocating((LocationManager) getSystemService(Context.LOCATION_SERVICE));
	}
	
	private void sendStateBroadcast(int state) {
		Intent intent = new Intent(SERVICE_STATE);
		intent.putExtra(SERVICE_STATE, state);
		sendBroadcast(intent);
	}

	private void shutdownSocket() throws IOException {
		if (bluetoothSocket.getInputStream() != null) {
			try {
				bluetoothSocket.getInputStream().close();
			} catch (Exception e) {}
		}
		
		if (bluetoothSocket.getOutputStream() != null) {
			try {
				bluetoothSocket.getOutputStream().close();
			} catch (Exception e) {}
		}
		
		try {
			bluetoothSocket.close();
		} catch (Exception e) {}
		
		bluetoothSocket = null;
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws IOException
	 */
	private void startConnection() throws IOException {

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

		new ConnectThread(bluetoothDevice, true);
		
		commandListener = new CommandListener(CarManager.instance().getCar());
		commandListener.createNewTrackIfNecessary();
		sendStateBroadcast(SERVICE_STARTING);
	}
	
	
	private void deviceDisconnected() {
		logger.info("Bluetooth device disconnected.");
		stopBackgroundService();
	}
	
	/**
	 * method gets called when the bluetooth device connection
	 * has been established. 
	 */
	private void deviceConnected() {
		logger.info("Bluetooth device connected.");
        // Service is running..
		sendStateBroadcast(SERVICE_STARTED);
		
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
		
		initializeCommandLooper(in, out);
	}

	protected void initializeCommandLooper(InputStream in, OutputStream out) {
		this.commandLooper = new OBDCommandLooper(
				in, out,
				this.commandListener, new ConnectionListener() {
					@Override
					public void onConnectionVerified() {
						BackgroundService.this.sendBroadcast(new Intent(CONNECTION_VERIFIED_INTENT));
					}
					
					@Override
					public void onConnectionException(IOException e) {
						BackgroundService.this.deviceDisconnected();
					}

					@Override
					public void onAllAdaptersFailed() {
						BackgroundService.this.onAllAdaptersFailed();
					}
				});
		this.commandLooper.start();
	}

	public void onAllAdaptersFailed() {
		stopBackgroundService();
		sendBroadcast(new Intent(CONNECTION_PERMANENTLY_FAILED_INTENT));		
	}
	
	private void openTroubleshootingActivity(int type) {
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
	
    private class ConnectThread extends Thread {
		private BluetoothAdapter adapter;
		private boolean secure;
		private BluetoothDevice device;
		private List<UUID> uuidCandidates;
		private int candidate;
		protected boolean started;

        public ConnectThread(BluetoothDevice device, boolean secure) {
        	logger.info("initiliasing connection to device "+device.getName() +" / "+ device.getAddress());
        	adapter = BluetoothAdapter.getDefaultAdapter();
            this.secure = secure;
            this.device = device;

            setName("BluetoothConnectThread");
            
            if (!startQueryingForUUIDs()) {
            	this.uuidCandidates = Collections.singletonList(EMBEDDED_BOARD_SPP);
            	this.start();
            	logger.info("UUID discovery for device not supported.");
            } else{
            	logger.info("Using UUID discovery mechanism.");
            }
            /*
             * it will start upon the broadcast receive otherwise
             */
        }
        
    	private boolean startQueryingForUUIDs() {
			Class<?> cl = BluetoothDevice.class;
			
			Class<?>[] par = {};
			Method fetchUuidsWithSdpMethod;
			try {
				fetchUuidsWithSdpMethod = cl.getMethod("fetchUuidsWithSdp", par);
			} catch (NoSuchMethodException e) {
				logger.warn(e.getMessage());
				return false;
			}
			
			Object[] args = {};
			try {
				BroadcastReceiver receiver = new BroadcastReceiver() {
				    @Override
				    public void onReceive(Context context, Intent intent) {
				        BluetoothDevice deviceExtra = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
				        Parcelable[] uuidExtra = intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
				        //Parse the UUIDs and get the one you are interested in
				        
				        logger.info("Found the following UUIDs for device "+deviceExtra.getName());
				        uuidCandidates = new ArrayList<UUID>();
				        for (Parcelable uuid : uuidExtra) {
				        	logger.info(uuid.toString());
				        	uuidCandidates.add(UUID.fromString(uuid.toString()));
						}

				        synchronized (ConnectThread.this) {
				        	if (!ConnectThread.this.started) {
				        		ConnectThread.this.start();
				        		ConnectThread.this.started = true;
				        		unregisterReceiver(this);
				        		logDeviceMetadata(deviceExtra);
				        	}
				        	
				        }
				    }

				};
				registerReceiver(receiver, new IntentFilter("android.bleutooth.device.action.UUID"));
				registerReceiver(receiver, new IntentFilter("android.bluetooth.device.action.UUID"));
				
				fetchUuidsWithSdpMethod.invoke(device, args);
			} catch (IllegalArgumentException e) {
				logger.warn(e.getMessage());
				return false;
			} catch (IllegalAccessException e) {
				logger.warn(e.getMessage());
				return false;
			} catch (InvocationTargetException e) {
				logger.warn(e.getMessage());
				return false;
			}			
			
			return true;
		}

        @SuppressLint("NewApi")
		protected void logDeviceMetadata(BluetoothDevice deviceExtra) {
        	String sep = System.getProperty("line.separator");
        	StringBuilder sb = new StringBuilder();
        	sb.append("Bluetooth Device '");
        	sb.append(deviceExtra.getName());
        	sb.append("'");
			sb.append(sep);
			sb.append("Address: ");
			sb.append(deviceExtra.getAddress());
			sb.append(sep);
			sb.append("Bond state: ");
			sb.append(deviceExtra.getBondState());
			sb.append(sep);
			try {
				if (deviceExtra.getClass().getMethod("getType", new Class<?>[] {}) != null) {
					sb.append("Type: ");
					sb.append(deviceExtra.getType());
				}
			} catch (NoSuchMethodException e) {
			}
			sb.append(sep);
			BluetoothClass clazz = deviceExtra.getBluetoothClass();
			sb.append("Class major version: ");
			sb.append(clazz.getMajorDeviceClass());
			sb.append(sep);
			sb.append("Class minor version: ");
			sb.append(clazz.getDeviceClass());
			sb.append(sep);
			sb.append("Class Contents: ");
			sb.append(clazz.describeContents());
			sb.append(sep);
			try {
				if (deviceExtra.getClass().getMethod("getUuids", new Class<?>[] {}) != null) {
					sb.append("UUIDs: ");
					sb.append(deviceExtra.getUuids());
				}
			} catch (NoSuchMethodException e) {
			}
			sb.append(sep);
			sb.append("Contents: ");
			sb.append(deviceExtra.describeContents());
			sb.append(sep);
			logger.info(sb.toString());
		}

		public void run() {
			boolean success = false;
			while (selectSocket()) {
            
	            if (bluetoothSocket == null) {
	            	logger.warn("Socket is null! Cancelling!");
	            	deviceDisconnected();
	                openTroubleshootingActivity(TroubleshootingActivity.BLUETOOTH_EXCEPTION);
	            }
	            
	            // Always cancel discovery because it will slow down a connection
	            adapter.cancelDiscovery();
	
	            // Make a connection to the BluetoothSocket
	            try {
					// This is a blocking call and will only return on a
	                // successful connection or an exception
            		bluetoothSocket.connect();
            		success = true;
	            	break;
		            		
	            } catch (IOException e) {
	                // Close the socket
	                try {
	                	shutdownSocket();
	                } catch (IOException e2) {
	                    logger.warn(e2.getMessage(), e2);
	                }
	            }
        	}
			
			if (success) {
				deviceConnected();
			} else {
				deviceDisconnected();
                openTroubleshootingActivity(TroubleshootingActivity.BLUETOOTH_EXCEPTION);
			}
        }

		private boolean selectSocket() {
			if (candidate >= uuidCandidates.size()) {
				return false;
			}
			
			BluetoothSocket tmp;
			UUID uuid = uuidCandidates.get(candidate++);
			logger.info("Attempting to connect to SDP "+ uuid);
			try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            uuid);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                    		uuid);
                }
                bluetoothSocket = tmp;
                return true;
            } catch (IOException e) {
            	logger.warn(e.getMessage() ,e);
            }
			
			return false;
		}

    }

}