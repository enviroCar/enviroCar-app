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

import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.protocol.AbstractOBDConnector;
import org.envirocar.app.protocol.AdapterConnectionNotYetEstablishedListener;
import org.envirocar.app.protocol.ELM327Connector;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;


/**
 * Connector Class for bluetooth service. Partly imported from Android OBD
 * Project
 * 
 * @author jakob
 * 
 */
public class ServiceConnector implements ServiceConnection, AdapterConnectionNotYetEstablishedListener {

	private static final int MAX_TRIES_PER_ADAPTER = 2;
	private BackgroundServiceInteractor interactor = null;
	private Listener localListener = null;
	private AbstractOBDConnector obdAdapter;
	private List<AbstractOBDConnector> adapterCandidates = new ArrayList<AbstractOBDConnector>();
	private int tries;
	private int adapterIndex;

	public ServiceConnector() {
		//TODO init through ServiceLoader (SlimServiceLoader...)
		adapterCandidates.add(new ELM327Connector());
		obdAdapter = adapterCandidates.get(0);
	}
	
	/**
	 * connects listener and monitor
	 */
	public void onServiceConnected(ComponentName componentName, IBinder binder) {
		interactor = (BackgroundServiceInteractor) binder;
		interactor.setListener(localListener);
	}

	/**
	 * deactivates the local monitor
	 */
	public void onServiceDisconnected(ComponentName name) {
		interactor = null;
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
	 */
	public void setServiceListener(Listener listener) {
		localListener = listener;
	}

	public void executeCommandRequests() {
		this.obdAdapter.executeRequestCommands(this);
	}

	public void executeInitializationSequence() {
		this.obdAdapter.executeInitializationSequence(this);
	}

	@Override
	public void connectionNotYetEstablished() {
		if (tries++ > MAX_TRIES_PER_ADAPTER) {
			if (++adapterIndex >= adapterCandidates.size()) {
				connectionFailed();
				return;
			}
			this.obdAdapter = adapterCandidates.get(adapterIndex % adapterCandidates.size());
			tries = 0;
		}
		this.obdAdapter.executeInitializationSequence(this);
	}

	private void connectionFailed() {
		localListener.connectionPermanentlyFailed();
	}
	

}