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
	private static final int MAX_TRIES_PER_ADAPTER = 1;
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
	private ConnectionListener connectionListener;
	private Object socketMutex;
	
	private Runnable commonCommandsRunnable = new Runnable() {
		public void run() {
			if (!running) {
				logger.info("Exiting commandHandler.");
				return;
			}
			logger.info("Executing Command Commands!");
			
			try {
				executeCommandRequests();
			} catch (IOException e) {
				running = false;
				connectionListener.onConnectionException(e);
				logger.info("Exiting commandHandler.");
				return;
			}
			
			if (!running) {
				logger.info("Exiting commandHandler.");
				return;
			}
			
			logger.info("Scheduling the Executiion of Command Commands!");
			commandExecutionHandler.postDelayed(commonCommandsRunnable, requestPeriod);
		}
	};


	private Runnable initializationCommandsRunnable = new Runnable() {
		public void run() {
			if (running && !connectionEstablished) {
				logger.info("Using "+obdAdapter.getClass().getName() +" connector.");
				try {
					executeInitializationRequests();
				} catch (IOException e) {
					running = false;
					connectionListener.onConnectionException(e);
					logger.info("Exiting commandHandler.");
					return;
				} catch (AdapterFailedException e) {
					logger.warn(e.getMessage());
				}
				
				if (!running) {
					logger.info("Exiting commandHandler.");
					return;
				}
				
				try {
					selectAdapter();
				} catch (AllAdaptersFailedException e) {
					connectionListener.onAllAdaptersFailed();
				}
				
				commandExecutionHandler.postDelayed(initializationCommandsRunnable, ADAPTER_TRY_PERIOD);
			}
		}

	};


	/**
	 * same as OBDCommandLooper#OBDCommandLooper(InputStream, OutputStream, Object, Listener, ConnectionListener, int) with NORM_PRIORITY
	 */
	public OBDCommandLooper(InputStream in, OutputStream out, Object socketMutex, String deviceName,
			Listener l, ConnectionListener cl) {
		this(in, out, socketMutex, deviceName, l, cl, NORM_PRIORITY);
	}
	

	/**
	 * An application shutting down the streams ({@link InputStream#close()} and
	 * the like) SHALL synchronize on the socketMutex object when doing so.
	 * Otherwise, the app might crash.
	 * 
	 * @param in the inputStream of the connection
	 * @param out the outputStream of the connection
	 * @param socketMutex the mutex object to use when shutting down the streams
	 * @param l the listener which receives command responses
	 * @param cl the connection listener which receives connection state changes
	 * @param priority thread priority
	 * @throws IllegalArgumentException if one of the inputs equals null
	 */
	public OBDCommandLooper(InputStream in, OutputStream out, Object socketMutex,
			String deviceName, Listener l, ConnectionListener cl, int priority) {
		super("OBD-CommandLooper-Handler", priority);
		
		if (in == null) throw new IllegalArgumentException("in must not be null!");
		if (out == null) throw new IllegalArgumentException("out must not be null!");
		if (socketMutex == null) throw new IllegalArgumentException("socketMutex must not be null!");
		if (l == null) throw new IllegalArgumentException("l must not be null!");
		if (cl == null) throw new IllegalArgumentException("cl must not be null!");
		
		this.inputStream = in;
		this.outputStream = out;
		this.socketMutex = socketMutex;
		
		this.commandListener = l;
		this.connectionListener = cl;
		
		adapterCandidates.add(new ELM327Connector());
		adapterCandidates.add(new AposW3Connector());
		adapterCandidates.add(new OBDLinkMXConnector());
		
		determinePreferredAdapter(deviceName);
	}
	
	private void determinePreferredAdapter(String deviceName) {
		for (AbstractOBDConnector ac : adapterCandidates) {
			if (ac.supportsDevice(deviceName)) {
				this.obdAdapter = ac;
				logger.info("Using "+this.obdAdapter.getClass().getName() +" connector.");
				return;
			}
		}
	}


	/**
	 * stop the command looper. this removes all pending commands.
	 * This object is no longer executable, a new instance has to
	 * be created.
	 */
	public void stopLooper() {
		logger.info("stopping the command execution!");
		this.running = false;
	}

	private void executeInitializationRequests() throws IOException, AdapterFailedException {
		List<CommonCommand> cmds = this.obdAdapter.getInitializationCommands();
		
		executeCommands(cmds);
	}

	private void executeCommandRequests() throws IOException {
		List<CommonCommand> cmds = this.obdAdapter.getRequestCommands();
		
		try {
			executeCommands(cmds);
		} catch (AdapterFailedException e) {
			logger.severe("This should never happen!", e);
		}
	}

	private void executeCommands(List<CommonCommand> cmds) throws IOException, AdapterFailedException {
		for (CommonCommand c : cmds) {
			verifyConnectionState();
			executeCommand(c);
		}
	}

	
	private void executeCommand(CommonCommand cmd) throws AdapterFailedException {
		try {
			if (cmd.getCommandState().equals(CommonCommandState.NEW)) {

				// Run the job
				cmd.setCommandState(CommonCommandState.RUNNING);
				synchronized (socketMutex) {
					cmd.run(inputStream, outputStream);
				}
			}
		} catch (IOException e) {
			if (!connectionEstablished) {
				/*
				 * lets first try a different adapter before we fail!
				 */
				throw new AdapterFailedException(this.obdAdapter.getClass().getName());
			} else {
				connectionListener.onConnectionException(e);
				running = false;
				return;	
			}
			
			
		} catch (Exception e) {
			logger.warn("Error while sending command '" + cmd.toString() + "'", e);
			cmd.setCommandState(CommonCommandState.EXECUTION_ERROR);
		}

		// Finished if no more job is in the waiting-list

		if (cmd != null) {
			if (cmd.getCommandState() == CommonCommandState.EXECUTION_ERROR) {
				logger.warn("Execution Error for" +cmd.getCommandName() +" / "+cmd.getCommand());
				return;
			}
			
			if (cmd.getCommandState() == CommonCommandState.UNMATCHED_RESULT) {
				logger.warn("Did not receive the expected result! Expected: "+cmd.getResponseByte());
				try {
					logger.info("Trying to read another command.");
					CommonCommand.readResponseLine(inputStream);
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
			
			if (cmd.getCommandState() == CommonCommandState.SEARCHING) {
				logger.info("Adapter still searching. Waiting a bit.");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
				}
				return;
			}
			cmd.setCommandState(CommonCommandState.FINISHED);

			if (!connectionEstablished && !cmd.isNoDataCommand()) {
				this.obdAdapter.processInitializationCommand(cmd);
				if (this.obdAdapter.connectionVerified()) {
					connectionEstablished();
				}
			}
			else {
				commandListener.receiveUpdate(cmd);
			}
		}
		
	}

	private void verifyConnectionState() throws IOException {
		if (this.inputStream == null || this.outputStream == null)
			throw new IOException("IO Streams not available.");
	}
	
	private void connectionEstablished() {
		logger.info("OBD Adapter " + this.obdAdapter.getClass().getName() +
				" verified the responses. Connection Established!");
		this.connectionEstablished = true;
		this.connectionListener.onConnectionVerified();
		
		/*
		 * switch to common command execution phase
		 */
		commandExecutionHandler.postDelayed(commonCommandsRunnable, requestPeriod);
	}


	private void selectAdapter() throws AllAdaptersFailedException {
		if (++tries >= MAX_TRIES_PER_ADAPTER) {
			if (adapterIndex+1 >= adapterCandidates.size()) {
				throw new AllAdaptersFailedException(adapterCandidates.toString());
			}
			this.obdAdapter = adapterCandidates.get(adapterIndex++ % adapterCandidates.size());
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
