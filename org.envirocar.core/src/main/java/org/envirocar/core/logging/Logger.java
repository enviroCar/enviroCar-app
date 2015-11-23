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

import org.envirocar.core.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Logging mechanism that logs to Androids built-in logging interface
 * and a local log file.
 * This class' methods emulate commonly used logging interfaces method syntax.
 * 
 * @author matthes rieke
 * 
 */
public class Logger {

	public static final int SEVERE = 1;
	public static final int WARNING = 2;
	public static final int INFO = 3;
	public static final int FINE = 4;
	public static final int VERBOSE = 5;
	public static final int DEBUG = 10;

	private static final String TAB_CHAR = "\t";

	private static List<Handler> handlers = new ArrayList<Handler>();
	private static int minimumLogLevel = INFO;
	
	static {
		try {
			handlers.add(getLocalFileHandler());
		} catch (Exception e) {
			Log.e(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
			handlers.add(new AndroidHandler());
		}
//		try {
//			handlers.add(new AndroidHandler());
//		} catch (Exception e) {
//			Log.e(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
//		}
	}

	public static Handler getLocalFileHandler() throws IOException {
		return new LocalFileHandler();
	}

	private String name;

	public Logger(String name) {
		this.name = name;
	}

	public static Logger getLogger(Class<?> claz) {
		return getLogger(claz.getName());
	}

	public static Logger getLogger(String name) {
		return new Logger(name);
	}

	protected final void log(int level, String message, Throwable e) {
		if (level > minimumLogLevel) {
			return;
		}
		
		String concatMessage;
		if (e != null) {
			concatMessage = createConcatenatedMessage(message, e);
		} else {
			concatMessage = message;
		}
		log(level, concatMessage);
	}

	private String createConcatenatedMessage(String message, Throwable e) {
		StringBuilder sb = new StringBuilder();
		sb.append(message);
		sb.append(":");
		sb.append(Util.NEW_LINE_CHAR);

		sb.append(convertExceptionToString(e));

		return sb.toString();
	}

	protected final void log(int level, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.name);
//		sb.append(":");
//		sb.append(Thread.currentThread().getStackTrace()[5].getLineNumber());
		sb.append("] ");
		sb.append(message);
		
		synchronized (Logger.class) {
			for (Handler h : handlers) {
				try {
					h.logMessage(level, sb.toString());
				} catch (RuntimeException e) {
					Log.e(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
				}
			}	
		}
		
	}

	public void debug(String message) {
		log(DEBUG, message, null);
	}

	public void verbose(String message) {
		log(VERBOSE, message, null);
	}

	public void fine(String message) {
		log(FINE, message, null);
	}

	public void info(String message) {
		log(INFO, message, null);
	}

	public void warn(String message) {
		log(WARNING, message, null);
	}

	public void warn(String message, Throwable e) {
		log(SEVERE, message, e);
	}

	public void severe(String message) {
		log(SEVERE, message, null);
	}

	public void severe(String message, Exception e) {
		log(SEVERE, message, e);
	}

	public void error(String message, Throwable t){
		log(SEVERE, message, t);
	}

	public static String convertExceptionToString(Throwable e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.getClass().getCanonicalName());
		sb.append(": ");
		sb.append(e.getMessage());
		sb.append("; StackTrace: ");
		sb.append(Util.NEW_LINE_CHAR);

		int count = 0;
		for (StackTraceElement ste : e.getStackTrace()) {
			sb.append(TAB_CHAR);
			sb.append(ste.toString());
			if (++count < e.getStackTrace().length)
				sb.append(Util.NEW_LINE_CHAR);
		}

		return sb.toString();
	}

	public static void initialize(String appVersion, boolean debugLogging) {
		if (debugLogging) {
			minimumLogLevel = DEBUG;
		}
		
		
		Logger initLogger = getLogger(Logger.class);
		StringBuilder sb = new StringBuilder();
		sb.append("System information:");
		sb.append(Util.NEW_LINE_CHAR);
		sb.append(System.getProperty("os.version"));
		sb.append(", ");
		sb.append(android.os.Build.VERSION.SDK_INT);
		sb.append(", ");
		sb.append(android.os.Build.DEVICE);
		sb.append(", ");
		sb.append(android.os.Build.MODEL);
		sb.append(", ");
		sb.append(android.os.Build.PRODUCT);
		sb.append("; App version: ");
		sb.append(appVersion);

		initLogger.info(sb.toString());
	}

	public boolean isEnabled(int level) {
		return level <= minimumLogLevel;
	}
}
