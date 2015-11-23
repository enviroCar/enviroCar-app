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
package org.envirocar.obd;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * TODO make unstatic
 */
public class FeatureFlags {

	/**
	 * use PID supported query to identify 
	 */
	public static final String PID_SUPPORTED_KEY = "pref_pid_supported";

	private static SharedPreferences prefs = null;

	public FeatureFlags(Context context){
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
//	public static void init(Context context) {
//		prefs = PreferenceManager.getDefaultSharedPreferences(context);
//	}


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
