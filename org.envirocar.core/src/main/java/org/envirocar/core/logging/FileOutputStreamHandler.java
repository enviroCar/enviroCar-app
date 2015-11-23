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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @deprecated This handler is no longer being used. Use LocalFileHandler instead.
 * @author matthes
 *
 */
@Deprecated
public class FileOutputStreamHandler implements Handler {

	private static final String SEVERE = "SEVERE";
	private static final String WARNING = "WARNING";
	private static final String INFO = "INFO";
	private static final String FINE = "FINE";
	private static final String VERBOSE = "VERBOSE";
	private static final String DEBUG = "DEBUG";
	private static DateFormat format = SimpleDateFormat.getDateTimeInstance();
	private FileOutputStream outputStream;
	private OutputStreamWriter writer;

	public FileOutputStreamHandler(FileOutputStream openFileOutput) {
		this.outputStream = openFileOutput;
		this.writer = new OutputStreamWriter(this.outputStream);
	}

	@Override
	public synchronized void logMessage(int level, String string) {
		Date now = new Date();
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(format.format(now));
		sb.append("] ");
		sb.append(getLevelAsString(level));
		sb.append(" :");
		sb.append(string);
		sb.append(Util.NEW_LINE_CHAR);

		try {
			writer.append(sb.toString());
			writer.flush();
		} catch (IOException e) {
			Log.e(AndroidHandler.DEFAULT_TAG, e.getMessage(), e);
		}
	}

	private Object getLevelAsString(int level) {
		switch (level) {
		case Logger.SEVERE:
			return SEVERE;
		case Logger.WARNING:
			return WARNING;
		case Logger.INFO:
			return INFO;
		case Logger.FINE:
			return FINE;
		case Logger.VERBOSE:
			return VERBOSE;
		case Logger.DEBUG:
			return DEBUG;
		default:
			return "";
		}
	}

	@Override
	public void initializeComplete() {
		
	}

}
