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

import org.envirocar.app.logging.Logger;

/**
 * Mass Air Flow Value PID 01 10
 * 
 * @author jakob
 * 
 */
public class MAF extends CommonCommand {
	
	private static final Logger logger = Logger.getLogger(MAF.class);

	public MAF() {
		super("01 10");
	}

	@Override
	public String getResult() {

		float maf = 0.0f;

		try {
			if (!"NODATA".equals(getRawData())) {
				int bytethree = buffer.get(2);
				int bytefour = buffer.get(3);
				maf = (bytethree * 256 + bytefour) / 100.0f;
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		return String.format("%.2f%s", maf, "");
	}

	@Override
	public String getCommandName() {
		return "Mass Air Flow";
	}
}