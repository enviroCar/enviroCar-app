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
import org.envirocar.app.commands.EchoOff;
import org.envirocar.app.commands.LineFeedOff;
import org.envirocar.app.commands.ObdReset;
import org.envirocar.app.commands.SelectAutoProtocol;
import org.envirocar.app.commands.Timeout;

public class ELM327Connector extends AbstractOBDConnector {

	@Override
	public void executeInitializationSequence(BackgroundServiceConnector serviceConnector) {
		serviceConnector.addJobToWaitingList(new ObdReset());
		serviceConnector.addJobToWaitingList(new EchoOff());
		serviceConnector.addJobToWaitingList(new EchoOff());
		serviceConnector.addJobToWaitingList(new LineFeedOff());
		serviceConnector.addJobToWaitingList(new Timeout(62));
		serviceConnector.addJobToWaitingList(new SelectAutoProtocol());		
	}

}
