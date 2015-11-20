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
package org.envirocar.core.logging;

import android.util.Log;

public class AndroidHandler implements Handler {
	
	public static final String DEFAULT_TAG = "enviroCar";

	@Override
	public void logMessage(int level, String msg) {
		switch (level) {
		case Logger.SEVERE:
			Log.wtf(DEFAULT_TAG, msg);
			break;
		case Logger.WARNING:
			Log.e(DEFAULT_TAG, msg);
			break;
		case Logger.INFO:
			Log.i(DEFAULT_TAG, msg);
			break;
		case Logger.FINE:
			Log.v(DEFAULT_TAG, msg);
			break;
		case Logger.VERBOSE:
			Log.v(DEFAULT_TAG, msg);
			break;
		case Logger.DEBUG:
			Log.d(DEFAULT_TAG, msg);
			break;
		default:
			Log.i(DEFAULT_TAG, msg);
			break;
		}
	}

	@Override
	public void initializeComplete() {
		
	}

}
