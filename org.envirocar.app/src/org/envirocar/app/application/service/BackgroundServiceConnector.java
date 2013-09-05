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

import org.envirocar.app.commands.CommonCommand;

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
public class BackgroundServiceConnector implements ServiceConnection {

	private BackgroundServiceInteractor interactor;

	public BackgroundServiceConnector() {
	}
	
	/**
	 * connects listener and monitor
	 */
	public void onServiceConnected(ComponentName componentName, IBinder binder) {
		interactor = (BackgroundServiceInteractor) binder;
		interactor.initializeConnection();
	}
	

	public void onServiceDisconnected(ComponentName name) {
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
	 * @deprecated this should not be the responsibility of the service connector!
	 */
	@Deprecated
	public void addJobToWaitingList(CommonCommand newJob) {
		if (null != interactor && interactor.isRunning())
			interactor.newJobToWaitingList(newJob);
	}


	public void shutdownBackgroundService() {
		this.interactor.shutdownConnection();
	}
	

}