/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Implements a {@link Logger} handler that writes to the Android log. The
 * implementation is rather straightforward. The name of the logger serves as
 * the log tag. Only the log levels need to be converted appropriately. For
 * this purpose, the following mapping is being used:
 *
 * <table>
 *   <tr>
 *     <th>logger level</th>
 *     <th>Android level</th>
 *   </tr>
 *   <tr>
 *     <td>
 *       SEVERE
 *     </td>
 *     <td>
 *       ERROR
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       WARNING
 *     </td>
 *     <td>
 *       WARN
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       INFO
 *     </td>
 *     <td>
 *       INFO
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       CONFIG
 *     </td>
 *     <td>
 *       DEBUG
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       FINE, FINER, FINEST
 *     </td>
 *     <td>
 *       VERBOSE
 *     </td>
 *   </tr>
 * </table>
 */
public class AndroidJULHandler extends Handler {
    /**
     * Holds the formatter for all Android log handlers.
     */
    private static final Formatter THE_FORMATTER = new Formatter() {
        @Override
        public String format(LogRecord r) {
            Throwable thrown = r.getThrown();
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                sw.write(r.getMessage());
                sw.write("\n");
                thrown.printStackTrace(pw);
                pw.flush();
                return sw.toString();
            } else {
                return r.getMessage();
            }
        }
    };

    /**
     * Constructs a new instance of the Android log handler.
     */
    public AndroidJULHandler() {
        setFormatter(THE_FORMATTER);
    }

    @Override
    public void close() {
        // No need to close, but must implement abstract method.
    }

    @Override
    public void flush() {
        // No need to flush, but must implement abstract method.
    }

    @Override
    public void publish(LogRecord record) {
        int level = getAndroidLevel(record.getLevel());
        String tag = AndroidHandler.DEFAULT_TAG;

        try {
            String message = getFormatter().format(record);
            Log.println(level, tag, message);
        } catch (RuntimeException e) {
            Log.e("AndroidHandler", "Error logging message.", e);
        }
    }

    public void publish(Logger source, String tag, Level level, String message) {
        // TODO: avoid ducking into native 2x; we aren't saving any formatter calls
        int priority = getAndroidLevel(level);

        try {
            Log.println(priority, tag, message);
        } catch (RuntimeException e) {
            Log.e("AndroidHandler", "Error logging message.", e);
        }
    }

    /**
     * Converts a {@link Logger} logging level into an Android one.
     *
     * @param level The {@link Logger} logging level.
     *
     * @return The resulting Android logging level.
     */
    static int getAndroidLevel(Level level) {
        int value = level.intValue();
        if (value >= 1000) { // SEVERE
            return Log.ERROR;
        } else if (value >= 900) { // WARNING
            return Log.WARN;
        } else if (value >= 800) { // INFO
            return Log.INFO;
        } else {
            return Log.DEBUG;
        }
    }
}
