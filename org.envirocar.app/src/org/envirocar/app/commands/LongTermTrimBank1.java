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
 * Long Term Fuel Trim (Cylinder) Bank 1 (PID 01 07)
 * 
 * @author jakob
 * 
 */
public class LongTermTrimBank1 extends NumberResultCommand {

	public LongTermTrimBank1() {
		super("01 07");
	}

	@Override
	public String getResult() {

		float fuelTrimValue = 0.0f;

		if (!"NODATA".equals(getRawData())) {
			int tmpValue = buffer.get(2);
			Double perc = (tmpValue - 128) * (100.0 / 128);
			fuelTrimValue = Float.parseFloat(perc.toString());
		}

		return String.format("%.2f%s", fuelTrimValue, "");
	}

	@Override
	public String getCommandName() {

		return "Long Term Fuel Trim Bank 1";
	}

}