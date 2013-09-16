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
 * Engine RPM on PID 01 0C
 * 
 * @author jakob
 * 
 */
public class RPM extends NumberResultCommand {

	public static final String NAME = "Engine RPM";
	private int rpm;

	public RPM() {
		super("01 0C");
	}

	@Override
	public String getResult() {

		if (!"NODATA".equals(getRawData())) {
			int bytethree = buffer[2];
			int bytefour = buffer[3];
			rpm = (bytethree * 256 + bytefour) / 4;
		}

		return String.format("%d%s", rpm, "");
	}

	@Override
	public String getCommandName() {
		return NAME;
	}

	@Override
	public Number getNumberResult() {
		return rpm;
	}

}