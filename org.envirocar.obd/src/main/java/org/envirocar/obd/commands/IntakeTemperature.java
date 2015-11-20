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

/**
 * Intake temperature on PID 01 0F
 * 
 * @author jakob
 * 
 */
public class IntakeTemperature extends NumberResultCommand {

	public static final String NAME = "Air Intake Temperature";
	private int temperature = Short.MIN_VALUE;

	public IntakeTemperature() {
		super("01 ".concat(PID.INTAKE_AIR_TEMP.toString()));
	}

	@Override
	public String getCommandName() {
		return NAME;
	}

	@Override
	public Number getNumberResult() {
		if (temperature == Short.MIN_VALUE) {
			int[] buffer = getBuffer();
			temperature = buffer[2] - 40;
		}
		return temperature;
	}

}