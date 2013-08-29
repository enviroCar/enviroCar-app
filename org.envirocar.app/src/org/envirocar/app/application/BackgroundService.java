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

package org.envirocar.app.application;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Service for connection to Bluetooth device and running commands. Imported
 * from Android OBD Reader project in some parts.
 * 
 * @author jakob
 * 
 */
public class BackgroundService extends Service {


	private static final Logger logger = Logger.getLogger(BackgroundService.class);
	// Properties

	private AtomicBoolean isTheServiceRunning = new AtomicBoolean(false);
	private AtomicBoolean isWaitingListRunning = new AtomicBoolean(false);
	private Long counter = 0L;
	
	// Bluetooth devices and connection items

	private BluetoothDevice bluetoothDevice = null;
	private BluetoothSocket bluetoothSocket = null;
	private static final UUID EMBEDDED_BOARD_SPP = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private Listener callbackListener = null;
	private final Binder binder = new LocalBinder();
	private BlockingQueue<CommonCommand> waitingList = new LinkedBlockingQueue<CommonCommand>();
	private BluetoothAdapter bluetoothAdapter;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
		logger.info("Stops the background service");
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.info("Starts the background service");
		startBackgroundService();
		return START_STICKY;
	}

	/**
	 * Starts the background service (bluetooth connction). Then calls methods
	 * to start sending the obd commands for initialization.
	 */
	private void startBackgroundService() {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		// Init bluetooth

		String remoteDevice = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);

		// Stop if device is not available

		if (remoteDevice == null || "".equals(remoteDevice)) {
			stopService();
		}

		try {

			bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			bluetoothDevice = bluetoothAdapter.getRemoteDevice(remoteDevice);

			startConnection();
		} catch (IOException e) {
			logger.warn("Connection to " + remoteDevice + " failed:", e);
			Toast.makeText(getApplicationContext(), "Connection to " + remoteDevice + " failed!", Toast.LENGTH_LONG).show();
			stopService();
		}
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws IOException
	 */
	private void startConnection() throws IOException {

		// Connect to bluetooth device

		ConnectThread t = new ConnectThread(bluetoothDevice, true);
		t.start();
		
		callbackListener.createNewTrackIfNecessary();

		/*
		 * EXTRACTED to AbstractOBDConnector
		 */
//		addCommandToWaitingList(new ObdReset());
//		addCommandToWaitingList(new EchoOff());
//		addCommandToWaitingList(new EchoOff());
//		addCommandToWaitingList(new LineFeedOff());
//		addCommandToWaitingList(new Timeout(62));
//		addCommandToWaitingList(new SelectAutoProtocol());
		
		/*
		 * This is what Torque does:
		 */

		// addCommandToWaitingList(new Defaults());
		// addCommandToWaitingList(new Defaults());
		// addCommandToWaitingList(new ObdReset());
		// addCommandToWaitingList(new ObdReset());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new LineFeedOff());
		// addCommandToWaitingList(new SpacesOff());
		// addCommandToWaitingList(new HeadersOff());
		// addCommandToWaitingList(new Defaults());
		// addCommandToWaitingList(new ObdReset());
		// addCommandToWaitingList(new ObdReset());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new EchoOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new MemoryOff());
		// addCommandToWaitingList(new LineFeedOff());
		// addCommandToWaitingList(new SpacesOff());
		// addCommandToWaitingList(new HeadersOff());
		// addCommandToWaitingList(new SelectAutoProtocol());
		// addCommandToWaitingList(new PIDSupported());
		// addCommandToWaitingList(new EnableHeaders());
		// addCommandToWaitingList(new PIDSupported());
		// addCommandToWaitingList(new HeadersOff());

		/*
		 * End Torque
		 */

		// Set waiting list execution counter
		counter = 0L;
	}
	
	/**
	 * method gets called when the bluetooth device connection
	 * has been established. 
	 */
	private void connected() {
		logger.info("Bluetooth device connected.");
        // Service is running..
		isTheServiceRunning.set(true);		
		callbackListener.onConnectionInitialized();
	}

	/**
	 * Add a command to the waiting-list
	 * 
	 * @param job
	 *            The command that should be added
	 * @return Counter in the waiting list
	 * @deprecated never used!
	 */
	@Deprecated
	public Long addCommandToWaitingList(CommonCommand job) {

		counter++;

		job.setCommandId(counter);
		try {
			waitingList.put(job);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
			job.setCommandState(CommonCommandState.QUEUE_ERROR);
		}

		return counter;
	}

	/**
	 * Method that stops the service, removes everything from the waiting list
	 */
	private void stopService() {

		waitingList.removeAll(waitingList);
		isWaitingListRunning.set(false);
		callbackListener.shutdown();
		isTheServiceRunning.set(false);
		
		try {
			bluetoothSocket.close();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		stopSelf();
	}


	/**
	 * Runs the waiting list until the service is stopped
	 */
	private void runWaitingList() {

		isWaitingListRunning.set(true);

		// Go through all the waiting-list-jobs

		while (!waitingList.isEmpty()) {

			CommonCommand currentJob = null;

			// Try to run the first job from the waitinglist

			try {

				currentJob = waitingList.take();

				if (currentJob.getCommandState().equals(CommonCommandState.NEW)) {

					// Run the job

					currentJob.setCommandState(CommonCommandState.RUNNING);
					currentJob.run(bluetoothSocket.getInputStream(),
							bluetoothSocket.getOutputStream());
				}
			} catch (Exception e) {
				logger.warn("Error while sending command '" + currentJob.toString() + "'", e);
				currentJob.setCommandState(CommonCommandState.EXECUTION_ERROR);
			}

			// Finished if no more job is in the waiting-list

			if (currentJob != null) {
				currentJob.setCommandState(CommonCommandState.FINISHED);
				if (callbackListener != null) {
					callbackListener.receiveUpdate(currentJob);
				}
			}
		}

		// Execution finished

		isWaitingListRunning.set(false);
	}
	
	/**
	 * Binder imported directly from Android OBD Project. Runs the waiting list
	 * when jobs are added to it
	 * 
	 * @author jakob
	 * 
	 */
	private class LocalBinder extends Binder implements BackgroundServiceInteractor {
	
		public void setListener(Listener callback) {
			callbackListener = callback;
		}

		public boolean isRunning() {
			return isTheServiceRunning.get();
		}

		public void newJobToWaitingList(CommonCommand job) {
			waitingList.add(job);

			if (!isWaitingListRunning.get())
				runWaitingList();
		}
	}
	
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private String socketType;
		private BluetoothAdapter adapter;

        public ConnectThread(BluetoothDevice device, boolean secure) {
        	adapter = BluetoothAdapter.getDefaultAdapter();
   		 // Unique UUID for this application
    	    UUID MY_UUID_SECURE =
    	        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    	    UUID MY_UUID_INSECURE =
    	        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
            BluetoothSocket tmp = null;
            socketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e("ec", "Socket Type: " + socketType + "create() failed", e);
            }
            socket = tmp;
        }

        public void run() {
            setName("ConnectThread" + socketType);
            logger.info("Running ConnectThread");

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    socket.close();
                } catch (IOException e2) {
                    logger.warn(e2.getMessage(), e2);
                }
                return;
            }

            connected();
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("ec", "close() of connect " + socketType + " socket failed", e);
            }
        }
    }

}