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
 * Short Term Trim (Cylinder) Bank 1, PID 01 06
 * 
 * @author jakob
 * 
 */
public class ShortTermTrimBank1 extends NumberResultCommand {

	private float fuelTrimValue = Float.NaN;

	public ShortTermTrimBank1() {
		super("01 06");
	}


	@Override
	public String getCommandName() {

		return "Short Term Fuel Trim Bank 1";
	}

	@Override
	public Number getNumberResult() {
		if (Float.isNaN(fuelTrimValue)) {
			int[] buffer = getBuffer();
			float tmpValue = buffer[2];
			fuelTrimValue = (float) ((tmpValue - 128) * (100.0 / 128));
		}
		return fuelTrimValue;
	}

}