/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package car.io.exception;

/**
 * Exception that is thrown when the location cannot be correct (latitude AND
 * longitude equal 0.0... this is somewhere in the ocean and therefore usually
 * an indicator that the GPS is not synced yet)f
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
		super(
				"Location Coordinates are invalid. Did you turn on GPS? Do you have a connection?");
	}
}
