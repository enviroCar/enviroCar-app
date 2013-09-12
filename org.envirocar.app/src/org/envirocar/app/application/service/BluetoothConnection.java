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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.envirocar.app.activity.TroubleshootingActivity;
import org.envirocar.app.logging.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;

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
	private BluetoothSocket bluetoothSocket;

    public BluetoothConnection(BluetoothDevice device, boolean secure, BackgroundService owner) {
    	logger.info("initiliasing connection to device "+device.getName() +" / "+ device.getAddress());
    	adapter = BluetoothAdapter.getDefaultAdapter();
    	this.owner = owner;
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

			        synchronized (BluetoothConnection.this) {
			        	if (!BluetoothConnection.this.started) {
			        		BluetoothConnection.this.start();
			        		BluetoothConnection.this.started = true;
			        		owner.unregisterReceiver(this);
			        	}
			        	
			        }
			    }

			};
			owner.registerReceiver(receiver, new IntentFilter("android.bleutooth.device.action.UUID"));
			owner.registerReceiver(receiver, new IntentFilter("android.bluetooth.device.action.UUID"));
			
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
		while (selectSocket()) {
        
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
        		bluetoothSocket.connect();
//            	alternativeConnect();
        		success = true;
            	break;
	            		
            } catch (IOException e) {
                // Close the socket
                try {
                	shutdownSocket(bluetoothSocket, new Object(), new Object());
                } catch (IOException e2) {
                    logger.warn(e2.getMessage(), e2);
                }
            }
    	}
		
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
            bluetoothSocket = tmp;
            return true;
        } catch (IOException e) {
        	logger.warn(e.getMessage() ,e);
        }
		
		return false;
	}
	
    private void alternativeConnect() throws IOException {
    	Class<?> rfSocketClass;
    	Object rfSocket;
    	try {
    		rfSocketClass = Class.forName("android.bluetooth.RfcommSocket");
            rfSocket = rfSocketClass.newInstance();
            rfSocketClass.getMethod("create", new Class<?>[0]).invoke(rfSocket, new Object[0]);	
    	}
    	catch (ClassNotFoundException e) {
    		throw new IOException(e);
    	} catch (InstantiationException e) {
    		throw new IOException(e);
		} catch (IllegalAccessException e) {
			throw new IOException(e);
		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		} catch (InvocationTargetException e) {
			throw new IOException(e);
		} catch (NoSuchMethodException e) {
			throw new IOException(e);
		}
    	
        try
        {
          Class<?>[] arrayOfClass = new Class[2];
          arrayOfClass[0] = String.class;
          arrayOfClass[1] = Integer.TYPE;
          Method localMethod = rfSocketClass.getMethod("connect", arrayOfClass);
          Object localObject = rfSocket;
          Object[] arrayOfObject = new Object[2]; 
          arrayOfObject[0] = bluetoothSocket.getRemoteDevice().getAddress();
          arrayOfObject[1] = Integer.valueOf(0);
          if (!((Boolean)localMethod.invoke(localObject, arrayOfObject)).booleanValue()) {
        	  throw new IOException("Can't connect to device " + bluetoothSocket.getRemoteDevice().getAddress());
          }
          OutputStream out = (OutputStream) rfSocketClass.getMethod("getOutputStream", new Class<?>[0]).invoke(localObject, new Object[0]);
          out.write("HI!".getBytes());
          out.flush();
          
        }
        catch (Exception e)
        {
          throw new IOException(e);
        }
    }
    
	public static void shutdownSocket(BluetoothSocket socket, Object inputMutex, Object outputMutex)
			throws IOException {
		synchronized (inputMutex) {
			logger.info("Shutting down bluetooth socket.");
			if (socket.getInputStream() != null) {
				try {
					socket.getInputStream().close();
				} catch (Exception e) {}
			}
		}
		
		synchronized (outputMutex) {
			if (socket.getOutputStream() != null) {
				try {
					socket.getOutputStream().close();
				} catch (Exception e) {}
			}
		}
		
		try {
			socket.close();
		} catch (Exception e) {}
		
	}

}