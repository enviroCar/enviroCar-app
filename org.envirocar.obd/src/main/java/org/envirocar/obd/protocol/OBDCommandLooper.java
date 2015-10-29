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
package org.envirocar.obd.protocol;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.Listener;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.protocol.drivedeck.DriveDeckSportConnector;
import org.envirocar.obd.protocol.exception.AllAdaptersFailedException;
import org.envirocar.obd.protocol.sequential.AposW3Connector;
import org.envirocar.obd.protocol.sequential.CarTrendConnector;
import org.envirocar.obd.protocol.sequential.ELM327Connector;
import org.envirocar.obd.protocol.sequential.OBDLinkMXConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.schedulers.Schedulers;

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
public class OBDCommandLooper {

	private enum Phase {
		INITIALIZATION,
		COMMAND_EXECUTION
	}
	
	private static final Logger logger = Logger.getLogger(OBDCommandLooper.class);
	protected static final long ADAPTER_TRY_PERIOD = 5000;
	public static final long MAX_NODATA_TIME = 1000 * 60 * 1;
	
	private List<OBDConnector> adapterCandidates = new ArrayList<OBDConnector>();
	private OBDConnector obdAdapter;
	private InputStream inputStream;
	private OutputStream outputStream;
	protected boolean running = true;
	private int adapterIndex;
	private ConnectionListener connectionListener;
	private String deviceName;
	private Map<Phase, AtomicInteger> phaseCountMap = new HashMap<Phase, AtomicInteger>();
	private MonitorRunnable monitor;
	private long lastSuccessfulCommandTime;
	private boolean userRequestedStop;


	/**
	 *
	 * @param in the inputStream of the connection
	 * @param out the outputStream of the connection
	 * @param l the listener which receives command responses
	 * @param cl the connection listener which receives connection state changes
	 * @throws IllegalArgumentException if one of the inputs equals null
	 */
	public OBDCommandLooper(InputStream in, OutputStream out,
			String deviceName, Listener l, ConnectionListener cl) {

		if (in == null) throw new IllegalArgumentException("in must not be null!");
		if (out == null) throw new IllegalArgumentException("out must not be null!");
		if (l == null) throw new IllegalArgumentException("l must not be null!");
		if (cl == null) throw new IllegalArgumentException("cl must not be null!");
		
		this.inputStream = in;
		this.outputStream = out;
		
		this.connectionListener = cl;
		
		this.deviceName = deviceName;
	
		this.phaseCountMap.put(Phase.INITIALIZATION, new AtomicInteger());
		this.phaseCountMap.put(Phase.COMMAND_EXECUTION, new AtomicInteger());

		setupAdapterCandidates();

		startPreferredAdapter(this.deviceName);
	}
	
	private void startPreferredAdapter(String deviceName) {
		for (OBDConnector ac : adapterCandidates) {
			if (ac.supportsDevice(deviceName)) {
				this.obdAdapter = ac;
				break;
			}
		}

		if (this.obdAdapter == null) {
			this.obdAdapter = adapterCandidates.get(0);
		}
		
		logger.info("Using " + this.obdAdapter.getClass().getSimpleName() + " connector as the preferred adapter.");
		startInitialization();
	}

	private void startInitialization() {
		this.obdAdapter.initialize(this.inputStream, this.outputStream)
				.subscribeOn(Schedulers.io())
				.subscribe(new Subscriber<Boolean>() {
					@Override
					public void onCompleted() {

					}

					@Override
					public void onError(Throwable e) {
						logger.warn("Adapter failed: " + obdAdapter.getClass().getSimpleName(), e);
						try {
							selectAdapter();
						} catch (AllAdaptersFailedException e1) {
							logger.warn("All Adapters failed", e1);
							connectionListener.onAllAdaptersFailed();
						}
					}

					@Override
					public void onNext(Boolean aBoolean) {
						startCollectingData();
					}
				});
	}

	private void startCollectingData() {
		this.obdAdapter.observe()
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.subscribe(new Subscriber<DataResponse>() {
					@Override
					public void onCompleted() {

					}

					@Override
					public void onError(Throwable e) {

					}

					@Override
					public void onNext(DataResponse dataResponse) {

					}
				});
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
		adapterCandidates.add(new CarTrendConnector());
		adapterCandidates.add(new AposW3Connector());
		adapterCandidates.add(new OBDLinkMXConnector());
		adapterCandidates.add(new DriveDeckSportConnector());
	}



	private void selectAdapter() throws AllAdaptersFailedException {
		if (this.obdAdapter == null) {
			startPreferredAdapter(deviceName);
		}

		if (adapterIndex+1 >= adapterCandidates.size()) {
			throw new AllAdaptersFailedException(adapterCandidates.toString());
		}

		this.obdAdapter = adapterCandidates.get(adapterIndex++ % adapterCandidates.size());
		startInitialization();
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
					commandExecutionHandler.getLooper().quit();
					
					connectionListener.requestConnectionRetry(new IOException("Waited too long for data."));
					return;
				}
			}
		}
		
	}


}
