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
package org.envirocar.app.protocol;

import org.envirocar.app.application.service.BackgroundServiceConnector;
import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;

public abstract class AbstractOBDConnector {

	public void executeRequestCommands(BackgroundServiceConnector serviceConnector) {
		addCommandstoWaitinglist(serviceConnector);
	}

	/**
	 * Helper method that adds the desired commands to the waiting list where
	 * all commands are executed
	 * @param serviceConnector 
	 */
	private void addCommandstoWaitinglist(BackgroundServiceConnector serviceConnector) {
		final CommonCommand speed = new Speed();
		final CommonCommand maf = new MAF();
		final CommonCommand rpm = new RPM();
		final CommonCommand intakePressure = new IntakePressure();
		final CommonCommand intakeTemperature = new IntakeTemperature();
		
		serviceConnector.addJobToWaitingList(speed);
		serviceConnector.addJobToWaitingList(maf);
		serviceConnector.addJobToWaitingList(rpm);
		serviceConnector.addJobToWaitingList(intakePressure);
		serviceConnector.addJobToWaitingList(intakeTemperature);
	}

	public abstract void executeInitializationSequence(BackgroundServiceConnector serviceConnector);
	
}
