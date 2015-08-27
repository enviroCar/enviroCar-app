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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.util.Util;

import android.util.Log;

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
		sb.append(":");
		sb.append(Thread.currentThread().getStackTrace()[5].getLineNumber());
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
		sb.append("; StackTracke: ");
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

}
