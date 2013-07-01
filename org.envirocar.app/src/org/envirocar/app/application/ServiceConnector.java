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
public class ServiceConnector implements ServiceConnection {

	private Monitor localMonitor = null;
	private Listener localListener = null;

	public void onServiceConnected(ComponentName componentName, IBinder binder) {
		localMonitor = (Monitor) binder;
		localMonitor.setListener(localListener);
	}

	public void onServiceDisconnected(ComponentName name) {
		localMonitor = null;
	}

	/**
	 * Check whether service is running
	 * 
	 * @return True if running
	 */
	public boolean isRunning() {
		if (localMonitor == null) {
			return localMonitor.isRunning();
		}

		return localMonitor.isRunning();
	}

	/**
	 * Add a new CommandJob to the waiting List
	 * 
	 * @param newJob
	 *            New CommandJob
	 */
	public void addJobToWaitingList(CommonCommand newJob) {
		if (null != localMonitor)
			localMonitor.newJobToWaitingList(newJob);
	}

	/**
	 * Set the Local Listener
	 * 
	 * @param listener
	 */
	public void setServiceListener(Listener listener) {
		localListener = listener;
	}

}