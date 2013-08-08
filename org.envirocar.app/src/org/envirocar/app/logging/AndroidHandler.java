/*
 * Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 * 
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.envirocar.app.logging;

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
