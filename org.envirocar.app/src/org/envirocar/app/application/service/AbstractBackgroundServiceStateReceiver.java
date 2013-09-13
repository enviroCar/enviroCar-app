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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class AbstractBackgroundServiceStateReceiver extends BroadcastReceiver {

	public static final String SERVICE_STATE = BackgroundServiceImpl.class.getName()+".STATE";
	public static final int SERVICE_STOPPED = 0;
	public static final int SERVICE_STARTING = 1;
	public static final int SERVICE_STARTED = 2;
	
	public static enum ServiceState {
		SERVICE_STOPPED, SERVICE_STARTING, SERVICE_STARTED, SERVICE_STOPPING;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals(SERVICE_STATE)) return;
		ServiceState state = (ServiceState) intent.getSerializableExtra(SERVICE_STATE);
		
		onStateChanged(state);
	}

	public abstract void onStateChanged(ServiceState state);

}
