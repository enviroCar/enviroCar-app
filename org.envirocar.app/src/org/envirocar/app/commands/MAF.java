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

import org.envirocar.app.commands.PIDUtil.PID;
import org.envirocar.app.logging.Logger;

/**
 * Mass Air Flow Value PID 01 10
 * 
 * @author jakob
 * 
 */
public class MAF extends NumberResultCommand {
	
	private static final Logger logger = Logger.getLogger(MAF.class);
	public static final String NAME = "Mass Air Flow";
	private float maf = Float.NaN;
	
	public MAF() {
		super("01 ".concat(PID.MAF.toString()));
	}


	@Override
	public String getCommandName() {
		return NAME;
	}

	@Override
	public Number getNumberResult() {
		if (Float.isNaN(maf)) {
			int[] buffer = getBuffer();
			try {
				if (getCommandState() != CommonCommandState.EXECUTION_ERROR) {
					int bytethree = buffer[2];
					int bytefour = buffer[3];
					maf = (bytethree * 256 + bytefour) / 100.0f;
				}
			} catch (IndexOutOfBoundsException ioobe){
				logger.warn("Get wrong result of the obd adapter");
			} catch (Exception e) {
				logger.warn("Error while creating the mass air flow value", e);
			}

		}
		return maf;
	}
}