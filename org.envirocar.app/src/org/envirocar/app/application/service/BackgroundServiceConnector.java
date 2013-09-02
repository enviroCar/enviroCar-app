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

import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.application.Listener;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.AbstractOBDConnector;
import org.envirocar.app.protocol.AdapterConnectionNotYetEstablishedListener;
import org.envirocar.app.protocol.ELM327Connector;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;


/**
 * Connector Class for bluetooth service. Partly imported from Android OBD
 * Project
 * 
 * @author jakob
 * 
 */
public class BackgroundServiceConnector implements ServiceConnection, AdapterConnectionNotYetEstablishedListener {

	private static final Logger logger = Logger.getLogger(BackgroundServiceConnector.class);
	private static final int MAX_TRIES_PER_ADAPTER = 2;
	private BackgroundServiceInteractor interactor;
	private Listener commandListener;
	private AbstractOBDConnector obdAdapter;
	private List<AbstractOBDConnector> adapterCandidates = new ArrayList<AbstractOBDConnector>();
	private int tries;
	private int adapterIndex;
	
	private Handler handler = new Handler();
	
	private Runnable waitingListRunnable = new Runnable() {
		public void run() {

			if (interactor.isRunning())
				executeCommandRequests();

			try {
				handler.postDelayed(waitingListRunnable, 500);
			} catch (NullPointerException e) {
				logger.severe("NullPointerException occured: Handler is null: " + (handler == null) + " waitingList is null: " + (waitingListRunnable == null), e);
			}
		}
	};

	public BackgroundServiceConnector(Listener listener) {
		this.commandListener = listener;
		//TODO init through ServiceLoader (SlimServiceLoader...)
		adapterCandidates.add(new ELM327Connector());
		obdAdapter = adapterCandidates.get(0);
	}
	
	/**
	 * connects listener and monitor
	 */
	public void onServiceConnected(ComponentName componentName, IBinder binder) {
		interactor = (BackgroundServiceInteractor) binder;
		interactor.setListener(commandListener);
		
		interactor.initializeConnection();
		
		try {
			handler.post(waitingListRunnable);
		} catch (Exception e) {
			logger.severe("NullPointerException occured: Handler is null: " + (handler == null) + " waitingList is null: " + (waitingListRunnable == null), e);
		}
	}
	

	public void onServiceDisconnected(ComponentName name) {
		interactor = null;
		handler.removeCallbacks(waitingListRunnable);
		interactor.shutdownConnection();
	}

	/**
	 * Check whether service is running
	 * 
	 * @return True if running
	 */
	public boolean isRunning() {
		if (interactor == null) {
			return false;
		}

		return interactor.isRunning();
	}

	/**
	 * Add a new CommandJob to the waiting List
	 * 
	 * @param newJob
	 *            New CommandJob
	 */
	public void addJobToWaitingList(CommonCommand newJob) {
		if (null != interactor)
			interactor.newJobToWaitingList(newJob);
	}

	/**
	 * Set the Local Listener
	 * 
	 * @param listener
	 * @deprecated use constructor instead
	 */
	@Deprecated
	public void setServiceListener(Listener listener) {
		commandListener = listener;
	}

	public void executeCommandRequests() {
		this.obdAdapter.executeRequestCommands(this);
	}

	public void executeInitializationSequence() {
		this.obdAdapter.executeInitializationSequence(this);
	}

	@Override
	public void connectionNotYetEstablished() {
		selectAdapter();
		this.obdAdapter.executeInitializationSequence(this);
	}

	protected void selectAdapter() {
		if (tries++ > MAX_TRIES_PER_ADAPTER) {
			if (++adapterIndex >= adapterCandidates.size()) {
				allAdaptersFailed();
				return;
			}
			this.obdAdapter = adapterCandidates.get(adapterIndex % adapterCandidates.size());
			tries = 0;
		}
	}

	private void allAdaptersFailed() {
		commandListener.connectionPermanentlyFailed();
	}
	

}