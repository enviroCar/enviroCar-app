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

import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.EchoOff;
import org.envirocar.app.commands.LineFeedOff;
import org.envirocar.app.commands.ObdReset;
import org.envirocar.app.commands.SelectAutoProtocol;
import org.envirocar.app.commands.Timeout;

public class ELM327Connector extends AbstractOBDConnector {

	/*
	 * This is what Torque does:
	 */

	// addCommandToWaitingList(new Defaults());
	// addCommandToWaitingList(new Defaults());
	// addCommandToWaitingList(new ObdReset());
	// addCommandToWaitingList(new ObdReset());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new LineFeedOff());
	// addCommandToWaitingList(new SpacesOff());
	// addCommandToWaitingList(new HeadersOff());
	// addCommandToWaitingList(new Defaults());
	// addCommandToWaitingList(new ObdReset());
	// addCommandToWaitingList(new ObdReset());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new EchoOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new MemoryOff());
	// addCommandToWaitingList(new LineFeedOff());
	// addCommandToWaitingList(new SpacesOff());
	// addCommandToWaitingList(new HeadersOff());
	// addCommandToWaitingList(new SelectAutoProtocol());
	// addCommandToWaitingList(new PIDSupported());
	// addCommandToWaitingList(new EnableHeaders());
	// addCommandToWaitingList(new PIDSupported());
	// addCommandToWaitingList(new HeadersOff());

	/*
	 * End Torque
	 */

	@Override
	public List<CommonCommand> getInitializationCommands() {
		List<CommonCommand> result = new ArrayList<CommonCommand>();
		result.add(new ObdReset());
		result.add(new EchoOff());
		result.add(new EchoOff());
		result.add(new LineFeedOff());
		result.add(new Timeout(62));
		result.add(new SelectAutoProtocol());
		return result;
	}

}
