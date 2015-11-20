/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views;

import org.envirocar.core.logging.Logger;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;

/**
 * Utility functions for the application
 * 
 */
public class Utils {
	
	private static final Logger logger = Logger.getLogger(Utils.class);


	public static int getActionBarId() {
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				return Class.forName("com.actionbarsherlock.R$id").getField("abs__action_bar_title").getInt(null);
			} else {
				// Use reflection to get the actionbar title TextView and set
				// the custom font. May break in updates.
				return Class.forName("com.android.internal.R$id").getField("action_bar_title").getInt(null);
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * Checks whether the GPS is enabled.
	 * 
	 * @param context
	 *            the current context
	 * @return true if gps is enabled.
	 */
	public static boolean isGPSEnabled(Context context) {
		return ((LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE) != null) && ((LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	/**
	 * Checks whether Bluetooth is enabled
	 * 
	 * @param context
	 *            the current context
	 * @return true if bluetooth is enabled
	 */
	public static boolean isBluetoothEnabled(Context context) {
		return BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled();
	}


}
