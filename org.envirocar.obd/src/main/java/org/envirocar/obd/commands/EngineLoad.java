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
 * EngineLoad Value on PID 01 04
 * 
 * @author jakob
 * 
 */
public class EngineLoad extends NumberResultCommand {

	private float value = Float.NaN;

	/**
	 * Create the Command
	 */
	public EngineLoad() {
		super("01 ".concat(PID.CALCULATED_ENGINE_LOAD.toString()));
	}

	@Override
	public String getCommandName() {
		return "Engine Load";
	}


	@Override
	public Number getNumberResult() {
		if (Float.isNaN(value)) {
			int[] buffer = getBuffer();
			value = (buffer[2] * 100.0f) / 255.0f;
		}
		return value;
	}

}