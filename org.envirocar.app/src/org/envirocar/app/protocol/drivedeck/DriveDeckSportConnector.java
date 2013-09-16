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
package org.envirocar.app.protocol.drivedeck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.AbstractAsynchronousConnector;
import org.envirocar.app.protocol.ResponseParser;
import org.envirocar.app.protocol.drivedeck.CycleCommand.PID;

public class DriveDeckSportConnector extends AbstractAsynchronousConnector {

	private static final Logger logger = Logger.getLogger(DriveDeckSportConnector.class);
	private static final char CARRIAGE_RETURN = '\r';
	static final char END_OF_LINE_RESPONSE = '>';
	private Mode mode = Mode.OFFLINE;
	private Protocol protocol;
	private String vin;
	private long firstConnectinResponse;
	private CycleCommand cycleCommand;
	private ResponseParser responseParser = new LocalResponseParser();
	
	private static enum Mode {
		OFFLINE, CONNECTING, CONNECTED
	}
	
	private static enum Protocol {
		CAN11500, CAN11250, CAN29500, CAN29250, KWP_SLOW, KWP_FAST, ISO9141
	}
	
	public DriveDeckSportConnector() {
		createCycleCommand();
		logger.info("Static CycleCommand: "+new String(cycleCommand.getOutgoingBytes()));
	}
	
	private void createCycleCommand() {
		List<PID> pidList = new ArrayList<PID>();
		pidList.add(PID.SPEED);
		pidList.add(PID.MAF);
//		pidList.add(PID.RPM);
//		pidList.add(PID.IAP);
//		pidList.add(PID.IAT);
		this.cycleCommand = new CycleCommand(pidList);
	}

	@Override
	public boolean connectionVerified() {
		return protocol != null && vin != null;
	}

	private void processDiscoveredControlUnits(String substring) {
		logger.info("Discovered CUs: "+ substring);
	}

	private void processSupportedPID(String substring) {
		logger.info("Supported PIDs: "+ substring);
	}

	private void processVIN(String vinInt) {
		this.vin = vinInt;
		logger.info("VIN is: "+this.vin);
	}

	private void determineProtocol(String protocolInt) {
		int prot = Integer.parseInt(protocolInt);
		switch (prot) {
		case 1:
			protocol = Protocol.CAN11500;
			break;
		case 2:
			protocol = Protocol.CAN11250;
			break;
		case 3:
			protocol = Protocol.CAN29500;
			break;
		case 4:
			protocol = Protocol.CAN29250;
			break;
		case 5:
			protocol = Protocol.KWP_SLOW;
			break;
		case 6:
			protocol = Protocol.KWP_FAST;
			break;
		case 7:
			protocol = Protocol.ISO9141;
			break;
		default:
			return;
		}

		logger.info("Protocol is: "+ protocol.toString());
		logger.info("Connected in "+ (System.currentTimeMillis() - firstConnectinResponse) +" ms.");
		mode = Mode.CONNECTED;
	}


	@Override
	public boolean supportsDevice(String deviceName) {
		return deviceName.contains("DRIVEDECK") && deviceName.contains("W4");
	}


	private CommonCommand parsePIDResponse(String pid,
			byte[] rawBytes, long now) {
		
		CommonCommand result = null;
		if (pid.equals("41")) {
			//Speed
			result = new SpeedDriveDeck();
		}
		else if (pid.equals("42")) {
			//MAF
			result = new MAFDriveDeck();
		}
		else if (pid.equals("52")) {
			//IAP
			result = new IntakeTemperature();
		}
		else if (pid.equals("49")) {
			//IAT
			result = new IntakePressure();
		}
		else if (pid.equals("40") || pid.equals("51")) {
			//RPM
			result = new RPM();
		}
		
		if (result != null) {
			result.setRawData(rawBytes);
			result.parseRawData();
			result.setCommandState(CommonCommandState.FINISHED);
			result.setResultTime(now);
		}
		
		return result;
	}

	@Override
	protected List<CommonCommand> getRequestCommands() {
		return Collections.singletonList((CommonCommand) cycleCommand);
	}


	@Override
	protected char getRequestEndOfLine() {
		return CARRIAGE_RETURN;
	}


	@Override
	protected ResponseParser getResponseParser() {
		return responseParser;
	}
	
	@Override
	protected List<CommonCommand> getInitializationCommands() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
		
		return Collections.singletonList((CommonCommand) new CommonCommand("") {
			@Override
			public void parseRawData() {
			}
			@Override
			public String getCommandName() {
				return "";
			}
			@Override
			public byte[] getOutgoingBytes() {
				return new byte[] {(byte) CARRIAGE_RETURN};
			}
		});
	}

	@Override
	public int getMaximumTriesForInitialization() {
		return 5;
	}


	private class LocalResponseParser implements ResponseParser {
		@Override
		public CommonCommand processResponse(byte[] bytes, int start, int end) {
			if (start >= end) return null;
			
			char type = (char) bytes[start+0];
			
			if (type == CycleCommand.RESPONSE_PREFIX_CHAR) {
				if ((char) bytes[start+4] == CycleCommand.TOKEN_SEPARATOR_CHAR) return null;
				
				String pid = new String(bytes, start+1, 2);
				
				/*
				 * METADATA Stuff
				 */
				if (pid.equals("14")) {
					mode = Mode.CONNECTING;
					logger.info("Mode: "+ mode.toString());
				}
				else if (pid.equals("15")) {
					processVIN(new String(bytes, start+3, end));
				}
				else if (pid.equals("70")) {
					processSupportedPID(new String(bytes, start+3, end));
				}
				else if (pid.equals("71")) {
					processDiscoveredControlUnits(new String(bytes, start+3, end));
				}
				
				else {
					/*
					 * A PID response
					 */
					long now = System.currentTimeMillis();
					
					byte[] pidResponseValue = new byte[4];
					pidResponseValue[0] = bytes[start+1];
					pidResponseValue[1] = bytes[start+2];
					int target;
					for (int i = start+4; i <= end; i++) {
						if ((char) bytes[i] == CycleCommand.TOKEN_SEPARATOR_CHAR)
							break;
						
						target = i-(start+4) + 2;
						if (target >= pidResponseValue.length) break;
						pidResponseValue[target] = bytes[i];
					}
					
					return parsePIDResponse(pid, pidResponseValue, now);
				}
				
			}
			else if (type == 'C') {
				determineProtocol(new String(bytes, start+1, end));
			}
			
			return null;
		}

		@Override
		public char getEndOfLine() {
			return END_OF_LINE_RESPONSE;
		}
	}

}
