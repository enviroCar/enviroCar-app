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

package org.envirocar.app.views;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;

/**
 * Utility functions for the application
 * 
 */
public class Utils {

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
			e.printStackTrace();
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

	/**
	 * Transform ISO 8601 string to Calendar.
	 * @param iso8601string 
	 * @return 
	 * @throws ParseException
	 */
	public static long isoDateToLong(final String iso8601string) throws ParseException {
		String s = iso8601string.replace("Z", "+00:00");
		try {
			s = s.substring(0, 22) + s.substring(23);
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException("Invalid length", 0);
		}
		Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);

		return date.getTime();
	}
	
	/**
	 * Returns the distance of two points in kilometers.
	 * 
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double getDistance(double lat1, double lng1, double lat2, double lng2) {

		double earthRadius = 6369;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		return dist;

	}

}
