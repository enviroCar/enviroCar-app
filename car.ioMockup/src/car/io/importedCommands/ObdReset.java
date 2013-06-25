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

package car.io.importedCommands;

import java.io.IOException;
import java.io.InputStream;

import car.io.commands.CommonCommand;

/**
 * This method will reset the OBD connection.
 */
public class ObdReset extends CommonCommand {

	public ObdReset() {
		super("AT Z");
	}

	/**
	 * Reset command returns an empty string, so we must override the following
	 * two methods.
	 * 
	 * @throws IOException
	 */
	@Override
	public void readResult(InputStream in) throws IOException {
		return;
	}

	@Override
	public String getRawData() {
		return "";
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Reset OBD";
	}

}