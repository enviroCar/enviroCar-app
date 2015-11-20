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

import android.annotation.SuppressLint;
import android.util.Log;

import org.envirocar.core.util.Util;

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
		if(effectiveFile == null)
			return null;
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
