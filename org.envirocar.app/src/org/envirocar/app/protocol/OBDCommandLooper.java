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
package org.envirocar.app.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.application.Listener;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * this is the main class for interacting with a OBD-II adapter.
 * It takes {@link InputStream} and {@link OutputStream} objects
 * to do the actual raw communication. A {@link Listener} is provided
 * with updates. The {@link ConnectionListener} will get informed on
 * certain changes in the connection state.
 * 
 * Initialize this class and simply use the {@link #start()} and {@link #stopLooper()}
 * methods to manage its state.
 * 
 * @author matthes rieke
 *
 */
public class OBDCommandLooper extends HandlerThread {

	private static final Logger logger = Logger.getLogger(OBDCommandLooper.class);
	private static final int MAX_TRIES_PER_ADAPTER = 2;
	protected static final long ADAPTER_TRY_PERIOD = 5000;
	
	private List<AbstractOBDConnector> adapterCandidates = new ArrayList<AbstractOBDConnector>();
	private AbstractOBDConnector obdAdapter;
	private Listener commandListener;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Handler commandExecutionHandler;
	protected boolean running = true;
	protected boolean connectionEstablished = false;
	protected long requestPeriod = 500;
	private int tries;
	private int adapterIndex;
	
	private Runnable commonCommandsRunnable = new Runnable() {
		public void run() {

			if (running) {
				try {
					executeCommandRequests();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
					running = false;
					connectionListener.onConnectionException(e);
					return;
				}
			}

			commandExecutionHandler.postDelayed(commonCommandsRunnable, requestPeriod);
		}
	};

	private Runnable initializationCommandsRunnable = new Runnable() {
		public void run() {
			if (running && !connectionEstablished) {
				try {
					try {
						selectAdapter();
					} catch (AllAdaptersFailedException e) {
						connectionListener.onAllAdaptersFailed();
					}
					executeInitializationRequests();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
					running = false;
					connectionListener.onConnectionException(e);
					return;
				}
				
				commandExecutionHandler.postDelayed(initializationCommandsRunnable, ADAPTER_TRY_PERIOD);
			}
		}
	};
	private ConnectionListener connectionListener;

	/**
	 * @see HandlerThread#HandlerThread(String)
	 */
	public OBDCommandLooper(String name, InputStream in, OutputStream out, Listener l, ConnectionListener cl) {
		this(name, in, out, l, cl, NORM_PRIORITY);
	}
	
	/**
	 * @param cl 
	 * @see HandlerThread#HandlerThread(String, int)
	 */
	public OBDCommandLooper(String name, InputStream in, OutputStream out, Listener l, ConnectionListener cl, int priority) {
		super(name, priority);
		
		adapterCandidates.add(new ELM327Connector());
		obdAdapter = adapterCandidates.get(0);
		
		this.inputStream = in;
		this.outputStream = out;
		
		this.commandListener = l;
		this.connectionListener = cl;
	}
	
	
	/**
	 * stop the command looper. this removes all pending commands.
	 * This object is no longer executable, a new instance has to
	 * be created.
	 */
	public void stopLooper() {
		this.running = false;
		commandExecutionHandler.removeCallbacks(commonCommandsRunnable);
		commandExecutionHandler.removeCallbacks(initializationCommandsRunnable);
		commandExecutionHandler.getLooper().quit();
	}

	private void executeInitializationRequests() throws IOException {
		List<CommonCommand> cmds = this.obdAdapter.getInitializationCommands();
		
		executeCommands(cmds);
	}

	private void executeCommandRequests() throws IOException {
		List<CommonCommand> cmds = this.obdAdapter.getRequestCommands();
		
		executeCommands(cmds);
	}

	private void executeCommands(List<CommonCommand> cmds) throws IOException {
		for (CommonCommand c : cmds) {
			verifyConnectionState();
			executeCommand(c);
		}
	}

	
	private void executeCommand(CommonCommand cmd) {
		try {
			if (cmd.getCommandState().equals(CommonCommandState.NEW)) {

				// Run the job
				cmd.setCommandState(CommonCommandState.RUNNING);
				cmd.run(inputStream, outputStream);
			}
		} catch (IOException e) {
			connectionListener.onConnectionException(e);
		} catch (Exception e) {
			logger.warn("Error while sending command '" + cmd.toString() + "'", e);
			cmd.setCommandState(CommonCommandState.EXECUTION_ERROR);
			return;
		}

		// Finished if no more job is in the waiting-list

		if (cmd != null) {
			cmd.setCommandState(CommonCommandState.FINISHED);
			if (commandListener != null) {
				commandListener.receiveUpdate(cmd);
			}
			
			if (!connectionEstablished && !cmd.isNoDataCommand()) {
				connectionEstablished();
			}
		}
		
	}

	private void verifyConnectionState() throws IOException {
		if (this.inputStream == null || this.outputStream == null)
			throw new IOException("IO Streams not available.");
	}
	
	private void connectionEstablished() {
		this.connectionEstablished = true;
		this.connectionListener.onConnectionVerified();
		
		/*
		 * switch to common command execution phase
		 */
		commandExecutionHandler.post(commonCommandsRunnable);
	}

	private void selectAdapter() throws AllAdaptersFailedException {
		if (tries++ > MAX_TRIES_PER_ADAPTER) {
			if (adapterIndex++ >= adapterCandidates.size()) {
				throw new AllAdaptersFailedException(adapterCandidates.toString());
			}
			this.obdAdapter = adapterCandidates.get(adapterIndex % adapterCandidates.size());
			tries = 0;
		}
	}
	
	@Override
	public void run() {
		Looper.prepare();
		commandExecutionHandler = new Handler();
		commandExecutionHandler.post(initializationCommandsRunnable);
		Looper.loop();
	}

}
