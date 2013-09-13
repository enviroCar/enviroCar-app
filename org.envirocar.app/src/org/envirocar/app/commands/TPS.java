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

package org.envirocar.app.commands;

/**
 * Throttle position on PID 01 11
 * 
 * @author jakob
 * 
 */
public class TPS extends NumberResultCommand {

	private float value;

	public TPS() {
		super("01 11");
	}

	@Override
	public String getCommandName() {
		return "Throttle Position";
	}

	@Override
	public String getResult() {
		String result = getRawData();

		if (!"NODATA".equals(result)) {
			value = (buffer[2] * 100.0f) / 255.0f;
			result = String.format("%.1f%s", value, "");
		}
		return result;
	}

	@Override
	public Number getNumberResult() {
		return value;
	}

}