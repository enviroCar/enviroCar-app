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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.envirocar.app.application.Listener;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.OBDConnector.ConnectionState;
import org.envirocar.app.protocol.drivedeck.DriveDeckSportConnector;
import org.envirocar.app.protocol.exception.AdapterFailedException;
import org.envirocar.app.protocol.exception.AllAdaptersFailedException;
import org.envirocar.app.protocol.exception.LooperStoppedException;
import org.envirocar.app.protocol.exception.ConnectionLostException;
import org.envirocar.app.protocol.sequential.AposW3Connector;
import org.envirocar.app.protocol.sequential.ELM327Connector;
import org.envirocar.app.protocol.sequential.OBDLinkMXConnector;

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

	private enum Phase {
		INITIALIZATION,
		COMMAND_EXECUTION
	}
	
	private static final Logger logger = Logger.getLogger(OBDCommandLooper.class);
	protected static final long ADAPTER_TRY_PERIOD = 5000;
	private static final Integer MAX_PHASE_COUNT = 2;
	public static final long MAX_NODATA_TIME = 1000 * 60 * 1;
	
	private List<OBDConnector> adapterCandidates = new ArrayList<OBDConnector>();
	private OBDConnector obdAdapter;
	private Listener commandListener;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Handler commandExecutionHandler;
	protected boolean running = true;
	protected boolean connectionEstablished = false;
	protected long requestPeriod = 100;
	private int tries;
	private int adapterIndex;
	private ConnectionListener connectionListener;
	private String deviceName;
	private Map<Phase, AtomicInteger> phaseCountMap = new HashMap<Phase, AtomicInteger>();
	private MonitorRunnable monitor;
	private long lastSuccessfulCommandTime;
	private boolean userRequestedStop;
	
	private Runnable commonCommandsRunnable = new Runnable() {
		public void run() {
			if (!running) {
				logger.info("Exiting commandHandler.");
				throw new LooperStoppedException();
			}
			
			try {
				executeCommandRequests();
			} catch (IOException e) {
				running = false;
				if (!userRequestedStop) {
					connectionListener.requestConnectionRetry(e);
				}
				logger.info("Exiting commandHandler.");
				throw new LooperStoppedException();
			}
			
			if (!running) {
				logger.info("Exiting commandHandler.");
				throw new LooperStoppedException();
			}
			
			commandExecutionHandler.postDelayed(commonCommandsRunnable, requestPeriod);
		}
	};


	private Runnable initializationCommandsRunnable = new Runnable() {
		public void run() {
			if (running && !connectionEstablished) {
				/*
				 * an async connector will probably only verify its connection
				 * after one try cycle of executeInitializationRequests.
				 */
				if (obdAdapter != null && obdAdapter.connectionState() == ConnectionState.CONNECTED) {
					connectionEstablished();
					return;
				}
				
				try {
					selectAdapter();
				} catch (AllAdaptersFailedException e) {
					running = false;
					connectionListener.onAllAdaptersFailed();
					throw new LooperStoppedException();
				}
				
				String stmt = "Trying "+obdAdapter.getClass().getSimpleName() +".";
				logger.info(stmt);
				connectionListener.onStatusUpdate(stmt);
			
				try {
					executeInitializationRequests();
				} catch (IOException e) {
					running = false;
					if (!userRequestedStop) {
						connectionListener.requestConnectionRetry(e);
					}
					logger.info("Exiting commandHandler.");
					throw new LooperStoppedException();
				} catch (AdapterFailedException e) {
					logger.warn(e.getMessage());
				}
				
				/*
				 * a sequential connector might already have a satisfied
				 * connection
				 */
				if (obdAdapter != null && obdAdapter.connectionState() == ConnectionState.CONNECTED) {
					connectionEstablished();
					return;
				}
				
				if (!running) {
					logger.info("Exiting commandHandler.");
					throw new LooperStoppedException();
				}
				
				commandExecutionHandler.postDelayed(initializationCommandsRunnable, ADAPTER_TRY_PERIOD);
			}
			
			if (!running) {
				throw new LooperStoppedException();
			}
		}

	};

	/**
	 * same as OBDCommandLooper#OBDCommandLooper(InputStream, OutputStream, Object, Listener, ConnectionListener, int) with NORM_PRIORITY
	 * @param outputMutex 
	 */
	public OBDCommandLooper(InputStream in, OutputStream out,
			String deviceName, Listener l, ConnectionListener cl) {
		this(in, out, deviceName, l, cl, NORM_PRIORITY);
	}
	

	/**
	 * An application shutting down the streams ({@link InputStream#close()} and
	 * the like) SHALL synchronize on the inputMutex object when doing so.
	 * Otherwise, the app might crash.
	 * 
	 * @param in the inputStream of the connection
	 * @param out the outputStream of the connection
	 * @param inputMutex the mutex object to use when shutting down the streams
	 * @param outputMutex 
	 * @param l the listener which receives command responses
	 * @param cl the connection listener which receives connection state changes
	 * @param priority thread priority
	 * @throws IllegalArgumentException if one of the inputs equals null
	 */
	public OBDCommandLooper(InputStream in, OutputStream out,
			String deviceName, Listener l, ConnectionListener cl, int priority) {
		super("OBD-CommandLooper-Handler", priority);
		
		if (in == null) throw new IllegalArgumentException("in must not be null!");
		if (out == null) throw new IllegalArgumentException("out must not be null!");
		if (l == null) throw new IllegalArgumentException("l must not be null!");
		if (cl == null) throw new IllegalArgumentException("cl must not be null!");
		
		this.inputStream = in;
		this.outputStream = out;
		
		this.commandListener = l;
		this.connectionListener = cl;
		
		this.deviceName = deviceName;
	
		this.phaseCountMap.put(Phase.INITIALIZATION, new AtomicInteger());
		this.phaseCountMap.put(Phase.COMMAND_EXECUTION, new AtomicInteger());
		
	}
	
	private void determinePreferredAdapter(String deviceName) {
		for (OBDConnector ac : adapterCandidates) {
			if (ac.supportsDevice(deviceName)) {
				this.obdAdapter = ac;
				break;
			}
		}

		if (this.obdAdapter == null) {
			this.obdAdapter = adapterCandidates.get(0);
		}
		
		this.obdAdapter.provideStreamObjects(inputStream, outputStream);
		logger.info("Using "+this.obdAdapter.getClass().getName() +" connector as the preferred adapter.");
	}


	/**
	 * stop the command looper. this removes all pending commands.
	 * This object is no longer executable, a new instance has to
	 * be created.
	 * 
	 * Only use this if the stop is from high-level (e.g. user request)
	 * and NOT on any kind of exception
	 */
	public void stopLooper() {
		logger.info("stopping the command execution!");
		this.running = false;
		this.userRequestedStop = true;
		
		if (this.monitor != null) {
			this.monitor.running = false;
		}
	}

	private void executeInitializationRequests() throws IOException, AdapterFailedException {
		try {
			this.obdAdapter.executeInitializationCommands();
		} catch (IOException e) {
			if (!userRequestedStop) {
				connectionListener.requestConnectionRetry(e);
			}
			running = false;
			return;
		}
		
	}

	private void executeCommandRequests() throws IOException {
		
		List<CommonCommand> cmds;
		try {
			cmds = this.obdAdapter.executeRequestCommands();
		} catch (ConnectionLostException e) {
			switchPhase(Phase.INITIALIZATION, new IOException(e));
			
			return;
		} catch (AdapterFailedException e) {
			logger.severe("This should never happen!", e);
			return;
		}
		
		long time = 0;
		for (CommonCommand cmd : cmds) {
			if (cmd.getCommandState() == CommonCommandState.FINISHED) {
				commandListener.receiveUpdate(cmd);
				time = cmd.getResultTime();
			}
		}
		
		if (time != 0) {
			lastSuccessfulCommandTime = time;
		}
		
	}

	
	private void switchPhase(Phase phase, IOException reason) {
		logger.info("Switching to Phase: " +phase + (reason != null ? " / Reason: "+reason.getMessage() : ""));
		
		
		int phaseCount = phaseCountMap.get(phase).incrementAndGet();
		
		commandExecutionHandler.removeCallbacks(initializationCommandsRunnable);
		commandExecutionHandler.removeCallbacks(commonCommandsRunnable);
		
		/*
		 * if we were too often in the same phase (e.g. init),
		 * request a reconnect
		 */
		if (phaseCount >= MAX_PHASE_COUNT) {
			logger.warn("Too often in phase: "+phaseCount);
			connectionListener.requestConnectionRetry(reason);
			
			running = false;
			return;
		}
		
		switch (phase) {
		case INITIALIZATION:
			connectionEstablished = false;
			obdAdapter = null;
			
			setupAdapterCandidates();
			
			commandExecutionHandler.post(initializationCommandsRunnable);
			break;
		case COMMAND_EXECUTION:
			this.connectionEstablished = true;
			this.connectionListener.onConnectionVerified();
			commandExecutionHandler.postDelayed(commonCommandsRunnable, requestPeriod);
			commandListener.onConnected(deviceName);
			
			startMonitoring();
			
			break;
		default:
			break;
		}
	}



	private void startMonitoring() {
		if (this.monitor != null) {
			this.monitor.running = false;
		}
		
		this.monitor = new MonitorRunnable();
		new Thread(this.monitor).start();
	}


	private void setupAdapterCandidates() {
		adapterCandidates.clear();
		adapterCandidates.add(new ELM327Connector());
		adapterCandidates.add(new AposW3Connector());
		adapterCandidates.add(new OBDLinkMXConnector());
		adapterCandidates.add(new DriveDeckSportConnector());
	}


	private void connectionEstablished() {
		logger.info("OBD Adapter " + this.obdAdapter.getClass().getName() +
				" verified the responses. Connection Established!");

		/*
		 * switch to common command execution phase
		 */
		switchPhase(Phase.COMMAND_EXECUTION, null);
	}


	private void selectAdapter() throws AllAdaptersFailedException {
		if (this.obdAdapter == null) {
			determinePreferredAdapter(deviceName);
			this.obdAdapter.provideStreamObjects(inputStream, outputStream);
		}
		
		else if (++tries >= this.obdAdapter.getMaximumTriesForInitialization()) {
			if (this.obdAdapter != null) {
				this.obdAdapter.prepareShutdown();
				this.obdAdapter.shutdown();
				if (this.obdAdapter.connectionState() == ConnectionState.CONNECTED) {
					/*
					 * the adapter was sure that it fits the device, so
					 * we do not need to try others
					 */
					throw new AllAdaptersFailedException(this.obdAdapter.getClass().getSimpleName());
				}
			}
			
			if (adapterIndex+1 >= adapterCandidates.size()) {
				throw new AllAdaptersFailedException(adapterCandidates.toString());
			}
			
			this.obdAdapter = adapterCandidates.get(adapterIndex++ % adapterCandidates.size());
			this.obdAdapter.provideStreamObjects(inputStream, outputStream);
			tries = 0;
		}
		
		if (this.obdAdapter != null) {
			this.requestPeriod = this.obdAdapter.getPreferredRequestPeriod();
		}
	}

	private class MonitorRunnable implements Runnable {

		private boolean running = true;
		
		@Override
		public void run() {
			Thread.currentThread().setName("OBD-Data-Monitor");
			while (running) {
				try {
					Thread.sleep(MAX_NODATA_TIME / 3);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
				}
				
				if (!running) return;
				
				if (System.currentTimeMillis() - lastSuccessfulCommandTime > MAX_NODATA_TIME) {
					commandExecutionHandler.removeCallbacks(commonCommandsRunnable);
					commandExecutionHandler.getLooper().quit();
					
					if (OBDCommandLooper.this.obdAdapter != null) {
						OBDCommandLooper.this.obdAdapter.shutdown();
					}
					
					connectionListener.requestConnectionRetry(new IOException("Waited too long for data."));
					return;
				}
			}
		}
		
	}
	
	@Override
	public void run() {
		Looper.prepare();
		logger.info("Command loop started. Hash:"+this.hashCode());
		commandExecutionHandler = new Handler();
		switchPhase(Phase.INITIALIZATION, null);
		try {
			Looper.loop();
		} catch (LooperStoppedException e) {
			logger.info("Command loop stopped. Hash:"+this.hashCode());
		}
		
		if (this.obdAdapter != null) {
			this.obdAdapter.shutdown();
		}
	}

}
