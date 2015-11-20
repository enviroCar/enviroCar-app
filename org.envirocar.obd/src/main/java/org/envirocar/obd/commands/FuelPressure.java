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

import org.envirocar.obd.commands.PIDUtil.PID;

public class FuelPressure extends NumberResultCommand {

	public static final String NAME = "Fuel Pressure";
	private int pressure = Short.MIN_VALUE;
	
	public FuelPressure() {
		super("01 ".concat(PID.FUEL_PRESSURE.toString()));
	}

	@Override
	public Number getNumberResult() {
		if (pressure == Short.MIN_VALUE) {
			pressure = getBuffer()[2] * 3;
		}
		return pressure;
	}

	@Override
	public String getCommandName() {
		return NAME;
	}

}
