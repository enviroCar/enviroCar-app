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
package org.envirocar.obd.commands;

/**
 * Long Term Fuel Trim (Cylinder) Bank 1 (PID 01 07)
 * 
 * @author jakob
 * 
 */
public class LongTermTrimBank1 extends NumberResultCommand {

	private double perc = Double.NaN;

	public LongTermTrimBank1() {
		super("01 07");
	}

	@Override
	public String getCommandName() {

		return "Long Term Fuel Trim Bank 1";
	}

	@Override
	public Number getNumberResult() {
		if (Double.isNaN(perc)) {
			int[] buffer = getBuffer();
			int tmpValue = buffer[2];
			perc = (tmpValue - 128) * (100d / 128d);
		}
		return perc;
	}

}