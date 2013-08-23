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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.envirocar.app.util.Util;

import android.util.Log;

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
