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

import org.envirocar.app.R;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.EchoOff;
import org.envirocar.app.commands.LineFeedOff;
import org.envirocar.app.commands.ObdReset;
import org.envirocar.app.commands.SelectAutoProtocol;
import org.envirocar.app.commands.Timeout;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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

/**
 * Service for connection to Bluetooth device and running commands. Imported
 * from Android OBD Reader project in some parts.
 * 
 * @author jakob
 * 
 */
public class BackgroundService extends Service {

	// Properties

	private AtomicBoolean isTheServiceRunning = new AtomicBoolean(false);
	private AtomicBoolean isWaitingListRunning = new AtomicBoolean(false);
	private Long counter = 0L;

	// Bluetooth devices and connection items

	private BluetoothDevice bluetoothDevice = null;
	private BluetoothSocket bluetoothSocket = null;
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private Listener callbackListener = null;
	private final Binder binder = new LocalBinder();
	private BlockingQueue<CommonCommand> waitingList = new LinkedBlockingQueue<CommonCommand>();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

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

		String remoteDevice = preferences.getString(
				org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY, null);

		// Stop if device is not available

		if (remoteDevice == null || "".equals(remoteDevice)) {
			stopService();
		}

		try {

			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			bluetoothDevice = bluetoothAdapter.getRemoteDevice(remoteDevice);
			bluetoothAdapter.cancelDiscovery();

			startConnection();
		} catch (Exception e) {
			stopService();
			Log.e("obd2", "retry " + e.toString());
		}
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws IOException
	 */
	private void startConnection() throws IOException {

		// Connect to bluetooth device

		bluetoothSocket = bluetoothDevice
				.createRfcommSocketToServiceRecord(MY_UUID);

		bluetoothSocket.connect();
		
		if(!bluetoothSocket.isConnected())
			return;
		
		((ECApplication) getApplication()).createNewTrackIfNecessary();

		addCommandToWaitingList(new ObdReset());
		addCommandToWaitingList(new EchoOff());
		addCommandToWaitingList(new EchoOff());
		addCommandToWaitingList(new LineFeedOff());
		addCommandToWaitingList(new Timeout(62));
		addCommandToWaitingList(new SelectAutoProtocol());
		
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

		// Service is running..
		isTheServiceRunning.set(true);

		// Set waiting list execution counter
		counter = 0L;

	}

	/**
	 * Add a command to the waiting-list
	 * 
	 * @param job
	 *            The command that should be added
	 * @return Counter in the waiting list
	 */
	public Long addCommandToWaitingList(CommonCommand job) {

		counter++;

		job.setCommandId(counter);
		try {
			waitingList.put(job);
		} catch (InterruptedException e) {
			job.setCommandState(CommonCommandState.QUEUE_ERROR);
		}

		return counter;
	}

	/**
	 * Method that stops the service, removes everything from the waiting list
	 */
	public void stopService() {

		waitingList.removeAll(waitingList);
		isWaitingListRunning.set(false);
		callbackListener = null;
		isTheServiceRunning.set(false);
		
		ECApplication application = (ECApplication) getApplication();
		application.stopLocating();

		try {
			bluetoothSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		stopSelf();
	}

	/**
	 * Binder imported directly from Android OBD Project. Runs the waiting list
	 * when jobs are added to it
	 * 
	 * @author jakob
	 * 
	 */
	public class LocalBinder extends Binder implements Monitor {
		public void setListener(Listener callback) {
			callbackListener = callback;
		}

		public boolean isRunning() {
			return isTheServiceRunning.get();
		}

		public void executeWaitingList() {
			runWaitingList();
		}

		public void newJobToWaitingList(CommonCommand job) {
			waitingList.add(job);

			if (!isWaitingListRunning.get())
				runWaitingList();
		}
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
				currentJob.setCommandState(CommonCommandState.EXECUTION_ERROR);
			}

			// Finished if no more job is in the waiting-list

			if (currentJob != null) {
				currentJob.setCommandState(CommonCommandState.FINISHED);
				callbackListener.receiveUpdate(currentJob);
			}
		}

		// Execution finished

		isWaitingListRunning.set(false);
	}

}