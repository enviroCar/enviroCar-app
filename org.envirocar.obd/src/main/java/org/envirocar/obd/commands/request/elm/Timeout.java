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

package org.envirocar.obd.commands.request.elm;


/**
 * This will set the value of time in milliseconds (ms) that the OBD interface
 * will wait for a response from the ECU. If exceeds, the response is "NO DATA".
 */
public class Timeout extends ConfigurationCommand {

	/**
	 * @param timeout
	 *            value between 0 and 255 that multiplied by 4 results in the
	 *            desired timeout in milliseconds (ms).
	 */
	public Timeout(int timeout) {
		super("AT ST " + Integer.toHexString(0xFF & timeout), Instance.TIMEOUT, true);
	}


}