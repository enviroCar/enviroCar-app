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
package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.Listener;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.protocol.ConnectionListener;
import org.envirocar.obd.protocol.drivedeck.DriveDeckSportConnector;
import org.envirocar.obd.protocol.exception.AllAdaptersFailedException;
import org.envirocar.obd.protocol.sequential.AposW3Connector;
import org.envirocar.obd.protocol.sequential.CarTrendConnector;
import org.envirocar.obd.protocol.sequential.ELM327Connector;
import org.envirocar.obd.protocol.sequential.OBDLinkMXConnector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * this is the main class for interacting with a OBD-II adapter.
 * It takes {@link InputStream} and {@link OutputStream} objects
 * to do the actual raw communication. A {@link Listener} is provided
 * with updates. The {@link ConnectionListener} will get informed on
 * certain changes in the connection state.
 * 
 * @author matthes rieke
 *
 */
public class OBDController {

	private static final Logger logger = Logger.getLogger(OBDController.class);
	protected static final long ADAPTER_TRY_PERIOD = 15000;
	public static final long MAX_NODATA_TIME = 10000;

	private Subscriber<DataResponse> dataSubscription;
	private Subscriber<Boolean> initialSubscriber;

	private List<OBDConnector> adapterCandidates = new ArrayList<OBDConnector>();
	private OBDConnector obdAdapter;
	private InputStream inputStream;
	private OutputStream outputStream;
	private int adapterIndex;
	private ConnectionListener connectionListener;
	private String deviceName;
	private long lastSuccessfulCommandTime;
	private boolean userRequestedStop = false;


	/**
	 *
	 * @param in the inputStream of the connection
	 * @param out the outputStream of the connection
	 * @param l the listener which receives command responses
	 * @param cl the connection listener which receives connection state changes
	 * @throws IllegalArgumentException if one of the inputs equals null
	 */
	public OBDController(InputStream in, OutputStream out,
			String deviceName, Listener l, ConnectionListener cl) {
		if (in == null) throw new IllegalArgumentException("in must not be null!");
		if (out == null) throw new IllegalArgumentException("out must not be null!");
		if (l == null) throw new IllegalArgumentException("l must not be null!");
		if (cl == null) throw new IllegalArgumentException("cl must not be null!");

		this.inputStream = in;
		this.outputStream = out;

		this.connectionListener = cl;

		this.deviceName = deviceName;
	
		setupAdapterCandidates();

		startPreferredAdapter();
	}

	private void setupAdapterCandidates() {
		adapterCandidates.clear();
		adapterCandidates.add(new ELM327Adapter());
		adapterCandidates.add(new CarTrendAdapter());
		adapterCandidates.add(new AposW3Connector());
		adapterCandidates.add(new OBDLinkMXConnector());
		adapterCandidates.add(new DriveDeckSportConnector());
	}

	private void startPreferredAdapter() {
		for (OBDConnector ac : adapterCandidates) {
			if (ac.supportsDevice(this.deviceName)) {
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

	private void selectNextAdapter() throws AllAdaptersFailedException {
		if (adapterIndex+1 >= adapterCandidates.size()) {
			throw new AllAdaptersFailedException(adapterCandidates.toString());
		}

		this.obdAdapter = adapterCandidates.get(adapterIndex++ % adapterCandidates.size());
		startInitialization();
	}

	/**
	 * start the init method of the adapter. This is used
	 * to bootstrap and verify the connection of the adapter
	 * with the ECU.
	 *
	 * The init times out fater a pre-defined period.
	 */
	private void startInitialization() {
		this.initialSubscriber = new Subscriber<Boolean>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				logger.warn("Adapter failed: " + obdAdapter.getClass().getSimpleName(), e);
				try {
					selectNextAdapter();
				} catch (AllAdaptersFailedException e1) {
					logger.warn("All Adapters failed", e1);
					connectionListener.onAllAdaptersFailed();
				}
			}

			@Override
			public void onNext(Boolean aBoolean) {
				initialSubscriber.unsubscribe();
				startCollectingData();
			}
		};

		this.obdAdapter.initialize(this.inputStream, this.outputStream)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.timeout(ADAPTER_TRY_PERIOD, TimeUnit.MILLISECONDS)
				.subscribe(this.initialSubscriber);
	}

	/**
	 * start the actual collection of data.
	 *
	 * the collection times out after a pre-defined period when no
	 * new data has arrived.
	 */
	private void startCollectingData() {
		/*
		 * inform the listener about the successful conn
		 */
		this.connectionListener.onConnectionVerified();

		this.dataSubscription = new Subscriber<DataResponse>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				/**
				 * check if this is a demanded stop: still this can lead to
				 * any kind of Exception (e.g. IOException)
				 */
				if (userRequestedStop) {
					dataSubscription.unsubscribe();
				}
			}

			@Override
			public void onNext(DataResponse dataResponse) {
				lastSuccessfulCommandTime = dataResponse.getTimestamp();
			}
		};

		this.obdAdapter.observe()
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.timeout(MAX_NODATA_TIME, TimeUnit.MILLISECONDS)
				.subscribe(this.dataSubscription);
	}


	/**
	 * @deprecated Use {@link #shutdown} instead
	 */
	@Deprecated
	public void stopLooper() {
		shutdown();
	}

	/**
	 * Shutdown the controller. this removes all pending commands.
	 * This object is no longer executable, a new instance has to
	 * be created.
	 *
	 * Only use this if the stop is from high-level (e.g. user request)
	 * and NOT on any kind of exception
	 */
	public void shutdown() {
		/**
		 * save that this is a stop on demand
		 */
		userRequestedStop = true;

		if (this.initialSubscriber != null) {
			this.initialSubscriber.unsubscribe();
		}
		if (this.dataSubscription != null) {
			this.dataSubscription.unsubscribe();
		}

	}


}
