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
package org.envirocar.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FeatureFlags {

	/**
	 * use PID supported query to identify 
	 */
	public static final String PID_SUPPORTED_KEY = "pref_pid_supported";
	private static SharedPreferences prefs;
	
	public static void init(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static boolean usePIDSupported() {
		return getFlagValue(PID_SUPPORTED_KEY);
	}

	private static boolean getFlagValue(String s) {
		if (prefs == null) {
			return false;
		}
		try {
			return prefs.getBoolean(s, false);
		}
		catch (RuntimeException e) {
		}
		catch (Error e) {
		}
		
		return false;
	}
	
}
