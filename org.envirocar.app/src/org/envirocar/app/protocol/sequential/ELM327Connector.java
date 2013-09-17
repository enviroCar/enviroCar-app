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
import java.util.Arrays;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.EchoOff;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.LineFeedOff;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.ObdReset;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.SelectAutoProtocol;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.commands.Timeout;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.AbstractAsynchronousConnector;
import org.envirocar.app.protocol.ResponseParser;


public class ELM327Connector extends AbstractAsynchronousConnector {
	
	private static final Logger logger = Logger.getLogger(ELM327Connector.class);
	private static final char COMMAND_SEND_END = '\r';
	private static final char COMMAND_RECEIVE_END = '>';
	private static final char COMMAND_RECEIVE_SPACE = ' ';
	
	protected int succesfulCount;
	private ResponseParser parser = new ELM327ResponseParser();
	private boolean disconnected;

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

	private void processInitializationCommand(String content) {
		logger.info("Processing init response: "+content);
		if (content.contains("ELM327v1.")) {
			succesfulCount++;
		}
		else if (content.contains("ATE0") && content.contains("OK")) {
			succesfulCount++;
		}
		else if (content.contains("OK")) {
			succesfulCount++;
		}
	}

	@Override
	public boolean connectionVerified() {
		return !disconnected && succesfulCount >= 5;
	}

	@Override
	public int getMaximumTriesForInitialization() {
		return 1;
	}

	@Override
	protected List<CommonCommand> getRequestCommands() {
		List<CommonCommand> result = new ArrayList<CommonCommand>();
		result.add(new Speed());
		result.add(new MAF());
		result.add(new RPM());
		result.add(new IntakePressure());
		result.add(new IntakeTemperature());
		return result;
	}

	@Override
	protected char getRequestEndOfLine() {
		return COMMAND_SEND_END;
	}

	@Override
	protected ResponseParser getResponseParser() {
		return parser;
	}


	private class ELM327ResponseParser implements ResponseParser {
		
		@Override
		public CommonCommand processResponse(byte[] bytes, int start, int count) {
			logger.info("processResponse: "+ new String(bytes, start, count));
			long now = System.currentTimeMillis();
			if (count < 2) return null;

			CommonCommand result = null;
			
			String prefix = new String(bytes, start+2, 2);
			
			if (prefix.equals("0D")) {
				result = new Speed();
			}
			else if (prefix.equals("10")) {
				result = new MAF();
			}
			else if (prefix.equals("0B")) {
				result = new IntakePressure();
			}
			else if (prefix.equals("0F")) {
				result = new IntakeTemperature();
			}
			else if (prefix.equals("0C")) {
				result = new RPM();
			}
			else {
				/*
				 * generic string result, maybe init response
				 */
				String content = new String(bytes, start, count);
				processInitializationCommand(content);				
			}
			
			if (result != null) {
				result.setRawData(createRawData(bytes, start, count));
				result.parseRawData();
				result.setResultTime(now);
			}
			
			return result;
		}

		private byte[] createRawData(byte[] bytes, int start, int count) {
			byte[] result = new byte[count];
			
			int targetIndex = 0;
			for (int i = 0; i < count; i++) {
				if (bytes[start+i] != COMMAND_RECEIVE_SPACE) {
					result[targetIndex++] = bytes[start+i];
				}
			}
			
			return Arrays.copyOf(result, targetIndex);
		}

		@Override
		public char getEndOfLine() {
			return COMMAND_RECEIVE_END;
		}

		@Override
		public void onDisconnected() {
			disconnected = true;
		}
		
	}

}
