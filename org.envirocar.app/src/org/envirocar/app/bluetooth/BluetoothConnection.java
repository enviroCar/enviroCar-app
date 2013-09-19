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
package org.envirocar.app.bluetooth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.envirocar.app.activity.TroubleshootingActivity;
import org.envirocar.app.application.service.BackgroundService;
import org.envirocar.app.bluetooth.FallbackBluetoothSocket.FallbackException;
import org.envirocar.app.logging.Logger;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;

@SuppressLint("NewApi")
public class BluetoothConnection extends Thread {

	private static final Logger logger = Logger.getLogger(BluetoothConnection.class);
	private static final UUID EMBEDDED_BOARD_SPP = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private BluetoothAdapter adapter;
	private boolean secure;
	private BluetoothDevice device;
	private List<UUID> uuidCandidates;
	private int candidate;
	protected boolean started;
	private UUID uuid;
	private BackgroundService owner;
	private BluetoothSocketWrapper bluetoothSocket;
	private Context context;
	private boolean running = true;

    public BluetoothConnection(BluetoothDevice device, boolean secure, BackgroundService owner,
    		Context ctx) {
    	logger.info("initiliasing connection to device "+device.getName() +" / "+ device.getAddress());
    	adapter = BluetoothAdapter.getDefaultAdapter();
    	this.owner = owner;
    	this.context = ctx;
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

			        uuidCandidates = new ArrayList<UUID>();
			        if (uuidExtra != null && uuidExtra.length > 0) {
			        	logger.info("Found the following UUIDs for device "+deviceExtra.getName());

				        for (Parcelable uuid : uuidExtra) {
				        	logger.info(uuid.toString());
				        	uuidCandidates.add(UUID.fromString(uuid.toString()));
						}	
			        }
			        
			        if (uuidCandidates.isEmpty()) {
			        	uuidCandidates.add(EMBEDDED_BOARD_SPP);
			        }

			        synchronized (BluetoothConnection.this) {
			        	if (!BluetoothConnection.this.started) {
			        		BluetoothConnection.this.start();
			        		BluetoothConnection.this.started = true;
			        		context.unregisterReceiver(this);
			        	}
			        	
			        }
			    }

			};
			context.registerReceiver(receiver, new IntentFilter("android.bleutooth.device.action.UUID"));
			context.registerReceiver(receiver, new IntentFilter("android.bluetooth.device.action.UUID"));
			
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


	public void run() {
		boolean success = false;
		while (running && selectSocket()) {
        
            if (bluetoothSocket == null) {
            	logger.warn("Socket is null! Cancelling!");
            	owner.deviceDisconnected();
            	owner.openTroubleshootingActivity(TroubleshootingActivity.BLUETOOTH_EXCEPTION);
            }
            
            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
				// This is a blocking call and will only return on a
                // successful connection or an exception
            	
            	logger.info("Connecting to socket...");
            	//TODO this might block VERY LONG! create a simple listening thread -> timeout -> call BackgroundService.deviceDisconnected()
        		bluetoothSocket.connect();
        		logger.info("Connected!");
        		success = true;
            	break;
	            		
            } catch (IOException e) {
            	//try the fallback
            	try {
					bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
					Thread.sleep(500);					
					bluetoothSocket.connect();
	        		success = true;
	            	break;
				} catch (FallbackException e1) {
					logger.warn("Could not initialize FallbackBluetoothSocket classes.", e);
				} catch (InterruptedException e1) {
					logger.warn(e1.getMessage(), e1);
				} catch (IOException e1) {
                	shutdownSocket(bluetoothSocket);
				}
			}
    	}
		
		if (!running) return;
		
		if (success) {
			owner.deviceConnected(bluetoothSocket);
		} else {
			owner.deviceDisconnected();
			owner.openTroubleshootingActivity(TroubleshootingActivity.BLUETOOTH_EXCEPTION);
		}
    }

	private boolean selectSocket() {
		if (candidate >= uuidCandidates.size()) {
			return false;
		}
		
		BluetoothSocket tmp;
		uuid = uuidCandidates.get(candidate++);
		logger.info("Attempting to connect to SDP "+ uuid);
		try {
            if (secure) {
                tmp = device.createRfcommSocketToServiceRecord(
                        uuid);
            } else {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                		uuid);
            }
            bluetoothSocket = new NativeBluetoothSocket(tmp);
            return true;
        } catch (IOException e) {
        	logger.warn(e.getMessage() ,e);
        }
		
		return false;
	}
	
    
	public static void shutdownSocket(BluetoothSocketWrapper socket) {
		logger.info("Shutting down bluetooth socket.");
		
		try {
			if (socket.getInputStream() != null) {
				socket.getInputStream().close();
			}
		} catch (Exception e) {}
		
	
		try {
			if (socket.getOutputStream() != null) {
				socket.getOutputStream().close();
			}
		} catch (Exception e) {}
		
		
		try {
			socket.close();
		} catch (Exception e) {}
		
	}

	public void cancelConnection() {
		running = false;
		shutdownSocket(bluetoothSocket);
	}

}