/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.envirocar.obd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class SimulatorService {
    // Debugging
    private static final String TAG = "SimulatorService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectedThread mConnectedThread;
    private int mState;
	private Random random = new Random();
	private float lastMaf;
	private boolean rpmServed;
	private boolean temperatureServed;
	private boolean pressureServed;
	private int rpm;
	private double temperature;
	private double pressure;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new OBDSimulator session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public SimulatorService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;

		//initial MAF
        int bytethree = (1+random.nextInt(8));
		int bytefour = (80+random.nextInt(19));
		
		lastMaf = (bytethree * 256 + bytefour) / 100.0f;
		calculateMAFValues(lastMaf);
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(OBDSimulator.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(OBDSimulator.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(OBDSimulator.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");


        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(OBDSimulator.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(OBDSimulator.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        byte[] buffer = "Device Disconnected!".getBytes();
        mHandler.obtainMessage(OBDSimulator.MESSAGE_READ, buffer.length, -1, buffer)
        	.sendToTarget();

        // Start the service over to restart listening mode
        SimulatorService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (SimulatorService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }



    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
		private int count;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    
                    int index = 0;
                    int i;
            		while ((i = mmInStream.read()) != -1) {
//            			if (count > 20) {
//            				try {
//    							Thread.sleep(11000);
//    							cancel();
//    							connectionLost();
//    							return;
//    						} catch (InterruptedException e) {
//    							e.printStackTrace();
//    						}
//            			}
            			byte b = (byte) i;
            			if (b == (byte) '\r') {
            				break;
            			}
            			
						buffer[index++] = b;
            		}

            		if (index == 0) continue;
            		
            		count++;
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(OBDSimulator.MESSAGE_READ, index, -1, buffer)
                            .sendToTarget();
//                    
//                    try {
//						Thread.sleep(10000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
                    
                    handleRequest(buffer, 0, index);
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    
                    try {
						mmInStream.close();
					} catch (IOException e1) {
	                    Log.e(TAG, "disconnected", e);
					}
                    
                    try {
						mmOutStream.close();
					} catch (IOException e1) {
	                    Log.e(TAG, "disconnected", e);
					}
                    
                    try {
						mmSocket.close();
					} catch (IOException e1) {
	                    Log.e(TAG, "disconnected", e);
					}
                    
                    connectionLost();
                    // Start the service over to restart listening mode
                    return;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(OBDSimulator.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


	public void handleRequest(byte[] buffer, int offset, int bytes) {
		String s = new String(buffer, offset, bytes).trim();
		Log.i("obdsim", "command received: "+s);
		
		String rawData = null;
		if (s.equals("AT Z")) {
			//ObdReset
		}
		else if (s.equals("AT E0")) {
			//echo off
			rawData = "ELM327v1.OKATE0";
		}
		else if (s.equals("AT L0")) {
			//Line feed off
			rawData = "OK";
		}
		else if (s.startsWith("AT SP")) {
			//select protocol
			rawData = "OK";
		}
		else if (s.startsWith("AT ST")) {
			//timeout
			rawData = "OK";
		}
		else if(s.startsWith("01 0D")) {
			//Speed
			rawData = "410D" + (23+random.nextInt(45));
		}
		else if (s.equals("01 10")) {
			int bytethree = (1+random.nextInt(8));
			int bytefour = (80+random.nextInt(19));
			//MAF
			rawData = "7F100"+ bytethree+""+ bytefour;
			lastMaf = (bytethree * 256 + bytefour) / 100.0f;
		}
		else if (s.equals("01 0B")) {
			//Pressure
			double press = getPressureFromLastMAFCalculation();
			String tmp = Integer.toHexString((int) press);
			if (tmp.length() == 1) tmp = "0"+tmp;
			rawData = "410B" +tmp; 
		}
		else if (s.equals("01 0F")) {
			//temp
			double temp = getTemperatureFromLastMAFCalcuation();
			String tmp = Integer.toHexString((int) (temp + 40));
			if (tmp.length() == 1) tmp = "0"+tmp;
			rawData = "410F" +tmp;
		}
		else if (s.equals("01 0C")) {
			//rpm
			int revols = getRPMFromLastMAFCalculation();
			rawData = integerToByteString(revols);
			if (rawData.length() != 8) {
				Log.i("obd-sim", rawData);	
			}
			else {
				rawData = "410C"+rawData.substring(4);
			}
		}
		else {
			String[] result = s.split(" ");
			rawData = "";
			for (String string : result) {
				rawData += string;
			}
		}
		
		if (rawData !=null) {
			String out = rawData.concat(">");

			mConnectedThread.write(out.getBytes());
		}
	}

	private static String integerToByteString(int revols) {
		int[] bytes = integerToByteArray(revols);
		StringBuilder sb = new StringBuilder();
		for (int b : bytes) {
			String tmp = Integer.toString(b, 16);
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
		}
		return sb.toString();
	}

	private static int[] integerToByteArray(int val) {
        int[] buffer = new int[4];
        
        buffer[0] = ((byte) (val >>> 24)) & 0xff;
        buffer[1] = ((byte) (val >>> 16)) & 0xff;
        buffer[2] = ((byte) (val >>> 8)) & 0xff;
        buffer[3] = ((byte) val) & 0xff;
        
        return buffer;
	}
	

	private int getRPMFromLastMAFCalculation() {
		checkCalculatedMAFValuesServed();
		
		rpmServed = true;
		return rpm;
	}


	private double getTemperatureFromLastMAFCalcuation() {
		checkCalculatedMAFValuesServed();
		
		temperatureServed = true;
		return temperature;
	}

	private double getPressureFromLastMAFCalculation() {
		checkCalculatedMAFValuesServed();
		
		pressureServed = true;
		return pressure;
	}
	
	private void checkCalculatedMAFValuesServed() {
		if (rpmServed && temperatureServed && pressureServed) {
			calculateMAFValues(lastMaf);
			rpmServed = false;
			temperatureServed = false;
			pressureServed = false;
		}
	}

	private void calculateMAFValues(float realMaf) {
		temperature = 26.0 + random.nextDouble()*4.0;
		pressure = 90.0 + random.nextInt(70);
		
		/*
		 * use static values for our simulated car
		 */
		double displacement = 1.15;
		int volumeEfficiencey = 85;
		
		double imap = (12000 * realMaf) / (volumeEfficiencey * displacement);
		
		rpm = (int) Math.floor(((temperature + 273.15d) * imap) / pressure);
	}
}
