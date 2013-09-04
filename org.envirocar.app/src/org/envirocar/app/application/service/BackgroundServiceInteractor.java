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

import org.envirocar.app.application.Listener;
import org.envirocar.app.commands.CommonCommand;

import android.content.Intent;

/**
 * Interface that adds jobs to the waiting list and executes it
 * 
 * @author jakob
 * 
 */
public interface BackgroundServiceInteractor {
	
	/**
	 * Set the listener for this monitor
	 * 
	 * @param listener
	 *            the listener
	 */
	void setListener(Listener listener);

	/**
	 * Check whether the monitor is running
	 * 
	 * @return true if monitori running
	 */
	boolean isRunning();
	
	/**
	 * this method shall create all required resources (e.g. bluetooth connection)
	 */
	void initializeConnection();

	
	/**
	 * this method shall free all resources created in {@link #initializeConnection()}
	 */
	void shutdownConnection();
	
	/**
	 * adds a new DommonCommand to the waiting list
	 * 
	 * @param newCommand
	 *            the new CommonCommand to add
	 * @deprecated this should not be the responsibility of the interactor!
	 */
	@Deprecated
	void newJobToWaitingList(CommonCommand newCommand);

	/**
	 * an implementation shall invoke the shutdown of the underlying service as
	 * we could not receive any data. An {@link Intent} with action {@link #CONNECTION_PERMANENTLY_FAILED_INTENT}
	 * shall be broadcasted.
	 */
	void allAdaptersFailed();


}