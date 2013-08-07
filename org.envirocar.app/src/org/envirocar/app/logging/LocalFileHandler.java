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

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import android.os.Environment;
import android.util.Log;

public class LocalFileHandler implements Handler {
	
	private static final int MAX_SIZE = 5242880; //5MB

	private java.util.logging.Logger logger;

	public LocalFileHandler(String localLogFile) throws Exception {
		this.logger = java.util.logging.Logger.getLogger("org.envirocar.app");
		String finalPath = ensureFileIsAvailable(localLogFile);
		this.logger.addHandler(createHandler(finalPath));
	}

	protected FileHandler createHandler(String finalPath) throws IOException {
		FileHandler h = new FileHandler(finalPath, MAX_SIZE, 5, true);
		h.setFormatter(new SimpleFormatter());
		return h;
	}

	private String ensureFileIsAvailable(String localLogFile) {
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + localLogFile);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			return file.toURI().getPath();
		} catch (IOException e) {
			Log.w(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
		}
		return localLogFile;
	}

	@Override
	public void logMessage(int level, String msg) {
		switch (level) {
		case Logger.SEVERE:
			this.logger.severe(msg);
			break;
		case Logger.WARNING:
			this.logger.warning(msg);
			break;
		case Logger.INFO:
			this.logger.info(msg);
			break;
		case Logger.FINE:
			this.logger.fine(msg);
			break;
		case Logger.VERBOSE:
			this.logger.finer(msg);
			break;
		case Logger.DEBUG:
			this.logger.finest(msg);
			break;
		default:
			this.logger.info(msg);
			break;
		}
	}

}
