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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.envirocar.app.util.Util;

import android.annotation.SuppressLint;
import android.util.Log;

public class LocalFileHandler implements Handler {
	
	private static final Logger LOG = Logger.getLogger(LocalFileHandler.class);
	
	private static final int MAX_SIZE = 5242880; //5MB
	
	public static final String LOCAL_LOG_FILE = "enviroCar-log.log";

	public static File effectiveFile;

	static {
		try {
			effectiveFile = Util.createFileOnExternalStorage(LOCAL_LOG_FILE);
		} catch (IOException e) {
			LOG.warn(e.getMessage(), e);
		}
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		java.util.logging.Handler[] handlers = rootLogger.getHandlers();
		for (java.util.logging.Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}
		LogManager.getLogManager().getLogger("").addHandler(new AndroidJULHandler());
		
	}

	
	private java.util.logging.Logger logger;

	public LocalFileHandler() throws IOException {
		this.logger = java.util.logging.Logger.getLogger("org.envirocar.app");
		String finalPath = ensureFileIsAvailable();
		this.logger.setLevel(Level.ALL);
		this.logger.addHandler(createHandler(finalPath));
	}
	
	@Override
	public void initializeComplete() {
		LOG.info("Using file "+ effectiveFile);
	}

	@SuppressLint("SimpleDateFormat")
	protected FileHandler createHandler(String finalPath) throws IOException {
		FileHandler h = new FileHandler(finalPath, MAX_SIZE, 3, true);
		
		final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		final String sep = System.getProperty("line.separator");
		
		h.setFormatter(new Formatter() {
			
			@Override
			public String format(LogRecord r) {
				String date = format.format(new Date(r.getMillis()));
				return String.format(Locale.US, "%s [%s]: (%d) %s%s", date, r.getLevel(), r.getThreadID(), r.getMessage(), sep);
			}
			
		});
		
		h.setLevel(Level.ALL);
		return h;
	}

	private String ensureFileIsAvailable() {
		try {
			if (!effectiveFile.exists()) {
				effectiveFile.createNewFile();
			}
			return effectiveFile.toURI().getPath();
		} catch (IOException e) {
			Log.w(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
		}
		
		File fallbackFile = new File(LOCAL_LOG_FILE);
		if (!fallbackFile.exists()) {
			try {
				fallbackFile.createNewFile();
				effectiveFile = fallbackFile;
				return effectiveFile.toURI().getPath();
			} catch (IOException e) {
				Log.w(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
			}
		}
		
		throw new IllegalStateException("Could not init file for "+ getClass().getSimpleName());
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
