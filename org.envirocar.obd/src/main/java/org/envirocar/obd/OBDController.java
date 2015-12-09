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
package org.envirocar.obd;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.adapter.AposW3Adapter;
import org.envirocar.obd.adapter.CarTrendAdapter;
import org.envirocar.obd.adapter.ELM327Adapter;
import org.envirocar.obd.adapter.OBDAdapter;
import org.envirocar.obd.adapter.OBDLinkMXAdapter;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.adapter.async.DriveDeckSportAdapter;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.envirocar.obd.exception.AllAdaptersFailedException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
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
	protected static final long ADAPTER_TRY_PERIOD = 20000;
	public static final long MAX_NODATA_TIME = 10000;
	private final Listener dataListener;

	private Subscriber<DataResponse> dataSubscription;
	private Subscriber<Boolean> initialSubscription;

	private Queue<OBDAdapter> adapterCandidates = new ArrayDeque<>();
	private OBDAdapter obdAdapter;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ConnectionListener connectionListener;
	private String deviceName;
	private boolean userRequestedStop = false;
	private boolean retried;
	private Bus eventBus;
	private Scheduler.Worker eventBusWorker;


	/**
	 * Init the OBD control layer with the streams and listeners to be used.
	 *
	 * @param in the inputStream of the connection
	 * @param out the outputStream of the connection
	 * @param l the listener which receives command responses
	 * @param cl the connection listener which receives connection state changes
	 */
	public OBDController(InputStream in, OutputStream out,
			String deviceName, Listener l, ConnectionListener cl) {
		this.inputStream = Preconditions.checkNotNull(in);
		this.outputStream = Preconditions.checkNotNull(out);

		this.connectionListener = Preconditions.checkNotNull(cl);
		this.dataListener = Preconditions.checkNotNull(l);

		this.deviceName = Preconditions.checkNotNull(deviceName);

		setupAdapterCandidates();

		startPreferredAdapter();

		if (this.eventBus != null) {
			this.eventBusWorker = Schedulers.computation().createWorker();
		}
	}

	/**
	 * setup the list of available Adapter implementations
	 */
	private void setupAdapterCandidates() {
		adapterCandidates.clear();
		adapterCandidates.offer(new ELM327Adapter());
		adapterCandidates.offer(new CarTrendAdapter());
		adapterCandidates.offer(new AposW3Adapter());
		adapterCandidates.offer(new OBDLinkMXAdapter());
		adapterCandidates.offer(new DriveDeckSportAdapter());
	}

	/**
	 * start the preferred adapter, determined by the device name
	 */
	private void startPreferredAdapter() {
		for (OBDAdapter ac : adapterCandidates) {
			if (ac.supportsDevice(this.deviceName)) {
				this.obdAdapter = ac;
				break;
			}
		}

		if (this.obdAdapter == null) {
			//poll the first instead
			this.obdAdapter = adapterCandidates.poll();
		}
		else {
			//remove the preferred from the queue so it is not used again
			this.adapterCandidates.remove(this.obdAdapter);
		}
		
		logger.info("Using " + this.obdAdapter.getClass().getSimpleName() + " connector as the preferred adapter.");
		startInitialization();
	}

	/**
	 * select the next adapter candidates from the list of implementations
	 *
	 * @throws AllAdaptersFailedException if the list has reached its end
	 */
	private void selectNextAdapter() throws AllAdaptersFailedException {
		this.retried = false;
		this.obdAdapter = adapterCandidates.poll();

		if (this.obdAdapter == null) {
			throw new AllAdaptersFailedException("All candidate adapters failed");
		}
	}

	/**
	 * start the init method of the adapter. This is used
	 * to bootstrap and verify the connection of the adapter
	 * with the ECU.
	 *
	 * The init times out fater a pre-defined period.
	 */
	private void startInitialization() {
		this.initialSubscription = new Subscriber<Boolean>() {
			@Override
			public void onCompleted() {
				this.unsubscribe();
			}

			@Override
			public void onError(Throwable e) {
				logger.warn("Adapter failed: " + obdAdapter.getClass().getSimpleName(), e);
				try {
					this.unsubscribe();

					if (obdAdapter.hasVerifiedConnection()) {

						if (!retried) {
							//one retry if it was verified!
							retried = true;
						}
						else {
							throw new AllAdaptersFailedException("Adapter verified a connection but could not establishe data: "
									+ obdAdapter.getClass().getSimpleName());
						}
					}
					else {
						selectNextAdapter();
					}

					/**
					 * try the selected adapter
					 */
					startInitialization();
				} catch (AllAdaptersFailedException e1) {
					logger.warn("All Adapters failed", e1);
					connectionListener.onAllAdaptersFailed();
					dataListener.shutdown();
				}
			}

			@Override
			public void onNext(Boolean b) {
				startCollectingData();
				dataListener.onConnected(deviceName);

				//unsubscribe, otherwise we will get a timeout
				initialSubscription.unsubscribe();
			}

		};

		/**
		 * start the observable and subscribe to it
		 */
		this.obdAdapter.initialize(this.inputStream, this.outputStream)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.timeout(ADAPTER_TRY_PERIOD, TimeUnit.MILLISECONDS)
				.subscribe(this.initialSubscription);
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
				this.unsubscribe();
				dataListener.shutdown();
			}

			@Override
			public void onError(Throwable e) {
				/**
				 * check if this is a demanded stop: still this can lead to
				 * any kind of Exception (e.g. IOException)
				 */
				if (userRequestedStop) {
					dataListener.shutdown();
				}

				this.unsubscribe();
			}

			@Override
			public void onNext(DataResponse dataResponse) {
				//lastSuccessfulCommandTime = dataResponse.getTimestamp();
				dataListener.receiveUpdate(dataResponse);
				pushToEventBus(dataResponse);
			}
		};

		/**
		 * start the observable with a timeout
		 */
		this.obdAdapter.observe()
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.timeout(MAX_NODATA_TIME, TimeUnit.MILLISECONDS)
				.subscribe(this.dataSubscription);
	}

	private void pushToEventBus(DataResponse dataResponse) {
		eventBusWorker.schedule(() -> {
            PropertyKeyEvent[] pkes = createEventsFromDataResponse(dataResponse);

            for (PropertyKeyEvent pke : pkes) {
                eventBus.post(pke);
            }
        });
	}

	protected PropertyKeyEvent[] createEventsFromDataResponse(DataResponse dataResponse) {
		PID pid = dataResponse.getPid();
		switch (pid) {
			case FUEL_SYSTEM_STATUS:
			case CALCULATED_ENGINE_LOAD:
			case SHORT_TERM_FUEL_TRIM_BANK_1:
			case LONG_TERM_FUEL_TRIM_BANK_1:
			case FUEL_PRESSURE:
			case INTAKE_MAP:
			case RPM:
			case SPEED:
			case INTAKE_AIR_TEMP:
			case MAF:
			case TPS:
				return new PropertyKeyEvent[] {
						new PropertyKeyEvent(PIDUtil.toPropertyKey(pid),
								dataResponse.getValue(), dataResponse.getTimestamp())
				};
			case O2_LAMBDA_PROBE_1_VOLTAGE:
			case O2_LAMBDA_PROBE_2_VOLTAGE:
			case O2_LAMBDA_PROBE_3_VOLTAGE:
			case O2_LAMBDA_PROBE_4_VOLTAGE:
			case O2_LAMBDA_PROBE_5_VOLTAGE:
			case O2_LAMBDA_PROBE_6_VOLTAGE:
			case O2_LAMBDA_PROBE_7_VOLTAGE:
			case O2_LAMBDA_PROBE_8_VOLTAGE:
				return new PropertyKeyEvent[] {
						new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER,
								dataResponse.getCompositeValues()[0], dataResponse.getTimestamp()),
						new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_VOLTAGE,
								dataResponse.getCompositeValues()[1], dataResponse.getTimestamp())
				};
			case O2_LAMBDA_PROBE_1_CURRENT:
			case O2_LAMBDA_PROBE_2_CURRENT:
			case O2_LAMBDA_PROBE_3_CURRENT:
			case O2_LAMBDA_PROBE_4_CURRENT:
			case O2_LAMBDA_PROBE_5_CURRENT:
			case O2_LAMBDA_PROBE_6_CURRENT:
			case O2_LAMBDA_PROBE_7_CURRENT:
			case O2_LAMBDA_PROBE_8_CURRENT:
				return new PropertyKeyEvent[] {
						new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_CURRENT_ER,
								dataResponse.getCompositeValues()[0], dataResponse.getTimestamp()),
						new PropertyKeyEvent(Measurement.PropertyKey.LAMBDA_CURRENT,
								dataResponse.getCompositeValues()[1], dataResponse.getTimestamp())
				};
		}

		return new PropertyKeyEvent[0];
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

		if (this.initialSubscription != null) {
			this.initialSubscription.unsubscribe();
		}
		if (this.dataSubscription != null) {
			this.dataSubscription.unsubscribe();
		}

	}


}
