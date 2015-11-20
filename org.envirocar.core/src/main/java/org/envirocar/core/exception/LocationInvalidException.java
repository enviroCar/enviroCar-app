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
package org.envirocar.core.exception;

/**
 * Exception that is thrown when the location cannot be correct (latitude AND
 * longitude equal 0.0... this is somewhere in the ocean and therefore usually
 * an indicator that the GPS is not synced yet)
 * 
 * @author jakob
 * 
 */

public class LocationInvalidException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -630826885585474670L;

	public LocationInvalidException() {
		super("Location Coordinates are invalid. Did you turn on GPS? Do you have a connection?");
	}
}
