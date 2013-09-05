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

import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.logging.Logger;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationUpdateListener implements LocationListener {

	private static final Logger logger = Logger.getLogger(LocationUpdateListener.class);
	private static LocationUpdateListener instance;
	
	public LocationUpdateListener() {
	}
	/**
	 * updates the location variables when the device moved
	 */
	@Override
	public void onLocationChanged(Location location) {
		EventBus.getInstance().fireEvent(new LocationEvent(location));
		logger.info("Get new position of " + location.getProvider() + " : lat " + location.getLatitude() + " long: " + location.getLongitude());
	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String arg0) {

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

	}

	public static synchronized LocationUpdateListener getInstance() {
		if (instance == null)
			instance = new LocationUpdateListener();
		return instance;
	}
	
	public static void startLocating(LocationManager lm) {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, getInstance());
	}
	
	public static void stopLocating(LocationManager lm) {
		lm.removeUpdates(getInstance());
	}
	
}
