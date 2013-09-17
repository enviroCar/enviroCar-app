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
package org.envirocar.app.protocol.sequential;

import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.EchoOff;
import org.envirocar.app.commands.LineFeedOff;
import org.envirocar.app.commands.ObdReset;
import org.envirocar.app.commands.SelectAutoProtocol;
import org.envirocar.app.commands.StringResultCommand;
import org.envirocar.app.commands.Timeout;
import org.envirocar.app.protocol.AbstractSequentialConnector;

public class ELM327Connector extends AbstractSequentialConnector {
	
	protected int succesfulCount;

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

	@Override
	public boolean supportsDevice(String deviceName) {
		return deviceName.contains("OBDII") || deviceName.contains("ELM327");
	}

	@Override
	public void processInitializationCommand(CommonCommand cmd) {
		if (cmd instanceof StringResultCommand) {
			String content = ((StringResultCommand) cmd).getStringResult();
			
			if (cmd instanceof EchoOff) {
				if (content.contains("ELM327v1.")) {
					succesfulCount++;
				}
				else if (content.contains("ATE0") && content.contains("OK")) {
					succesfulCount++;
				}
			}
			
			else if (cmd instanceof LineFeedOff) {
				if (content.contains("OK")) {
					succesfulCount++;
				}
			}
			
			else if (cmd instanceof Timeout) {
				if (content.contains("OK")) {
					succesfulCount++;
				}
			}
			
			else if (cmd instanceof SelectAutoProtocol) {
				if (content.contains("OK")) {
					succesfulCount++;
				}
			}
		}
		
	}

	@Override
	public ConnectionState connectionState() {
		if (succesfulCount >= 5) {
			return ConnectionState.CONNECTED;
		}
		return ConnectionState.DISCONNECTED;
	}

	@Override
	public void shutdown() {
		
	}

	@Override
	public int getMaximumTriesForInitialization() {
		return 1;
	}

	@Override
	public void prepareShutdown() {
		// TODO Auto-generated method stub
		
	}



}
