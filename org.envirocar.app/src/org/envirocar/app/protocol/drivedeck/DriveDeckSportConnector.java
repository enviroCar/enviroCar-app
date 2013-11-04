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
import java.util.Locale;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.O2LambdaProbe;
import org.envirocar.app.commands.PIDSupported;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.AbstractAsynchronousConnector;
import org.envirocar.app.protocol.ResponseParser;
import org.envirocar.app.protocol.drivedeck.CycleCommand.PID;

public class DriveDeckSportConnector extends AbstractAsynchronousConnector {

	private static final Logger logger = Logger.getLogger(DriveDeckSportConnector.class);
	private static final char CARRIAGE_RETURN = '\r';
	static final char END_OF_LINE_RESPONSE = '>';
	private Protocol protocol;
	private String vin;
	private long firstConnectionResponse;
	private CycleCommand cycleCommand;
	private ResponseParser responseParser = new LocalResponseParser();
	private ConnectionState state = ConnectionState.DISCONNECTED;
	private boolean send;
	
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
		pidList.add(PID.RPM);
		pidList.add(PID.IAP);
		pidList.add(PID.IAT);
//		pidList.add(PID.SHORT_TERM_FUEL_TRIM);
//		pidList.add(PID.LONG_TERM_FUEL_TRIM);
		pidList.add(PID.O2_LAMBDA_PROBE_1_VOLTAGE);
		this.cycleCommand = new CycleCommand(pidList);
	}

	@Override
	public ConnectionState connectionState() {
		return this.state;
	}

	private void processDiscoveredControlUnits(String substring) {
		logger.info("Discovered CUs... ");
	}

	protected void processSupportedPID(byte[] bytes, int start, int count) {
		String group = new String(bytes, start+6, 2);
		
		if (group.equals("00")) {
			/*
			 * this is the first group containing the PIDs of major interest
			 */
			PIDSupported pidCmd = new PIDSupported();
			byte[] rawBytes = new byte[12];
			rawBytes[0] = '4';
			rawBytes[1] = '1';
			rawBytes[2] = (byte) pidCmd.getResponseTypeID().charAt(0);
			rawBytes[3] = (byte) pidCmd.getResponseTypeID().charAt(1);
			int target = 4;
			String hexTmp;
			for (int i = 9; i < 14; i++) {
				if (i == 11) continue;
				hexTmp = oneByteToHex(bytes[i+start]);
				rawBytes[target++] = (byte) hexTmp.charAt(0);
				rawBytes[target++] = (byte) hexTmp.charAt(1);
			}
			
			pidCmd.setRawData(rawBytes);
			pidCmd.parseRawData();
			logger.info(pidCmd.getSupportedPIDs().toArray().toString());
		}
	}

	private String oneByteToHex(byte b) {
		String result = Integer.toString(b, 16).toUpperCase(Locale.US);
		if (result.length() == 1) result = "0".concat(result);
		return result;
	}

	private void processVIN(String vinInt) {
		this.vin = vinInt;
		logger.info("VIN is: "+this.vin);
		
		updateConnectionState();
	}

	private void updateConnectionState() {
		if (state == ConnectionState.VERIFIED) {
			return;
		}
		
		if (protocol != null || vin != null) {
			state = ConnectionState.CONNECTED;
		}
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
		logger.info("Connected in "+ (System.currentTimeMillis() - firstConnectionResponse) +" ms.");
		
		updateConnectionState();
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
			result = new Speed();
		}
		else if (pid.equals("42")) {
			//MAF
			result = new MAF();
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
		else if (pid.equals("4D")) {
			//TODO the current manual does not provide info on how to
			//determine which probe value is returned.
			result = O2LambdaProbe.fromPIDEnum(org.envirocar.app.commands.PIDUtil.PID.O2_LAMBDA_PROBE_1_VOLTAGE);
		}
		else {
			logger.info("Parsing not yet supported for response:" +pid);
		}
		
		if (result != null) {
			byte[] rawData = createRawData(rawBytes, result.getResponseTypeID());
			result.setRawData(rawData);
			result.parseRawData();
			
			if (result.getCommandState() == CommonCommandState.EXECUTION_ERROR ||
					result.getCommandState() == CommonCommandState.SEARCHING) {
				return null;
			}
			
			result.setCommandState(CommonCommandState.FINISHED);
			result.setResultTime(now);
			this.state = ConnectionState.VERIFIED;
		}
		
		return result;
	}

	private byte[] createRawData(byte[] rawBytes, String type) {
		byte[] result = new byte[4 + rawBytes.length*2];
		byte[] typeBytes = type.getBytes();
		result[0] = (byte) '4';
		result[1] = (byte) '1';
		result[2] = typeBytes[0];
		result[3] = typeBytes[1];
		for (int i = 0; i < rawBytes.length; i++) {
			String hex = byteToHex(rawBytes[i]);
			result[(i*2)+4] = (byte) hex.charAt(0);
			result[(i*2)+1+4] = (byte) hex.charAt(1);
		}
		return result;
	}

	private String byteToHex(byte b) {
		String result = Integer.toString((int) b, 16);
		if (result.length() == 1) {
			result = "0".concat(result);
		}
		return result;
	}

	@Override
	protected List<CommonCommand> getRequestCommands() {
		if (!send) {
			send = true;
			return Collections.singletonList((CommonCommand) cycleCommand);
		}
		else {
			return Collections.emptyList();
		}
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
		
		return Collections.singletonList((CommonCommand) new CarriageReturnCommand());
	}

	@Override
	public int getMaximumTriesForInitialization() {
		return 15;
	}


	private class LocalResponseParser implements ResponseParser {
		@Override
		public CommonCommand processResponse(byte[] bytes, int start, int count) {
			if (count <= 0) return null;
			
			char type = (char) bytes[start+0];
			
			if (type == CycleCommand.RESPONSE_PREFIX_CHAR) {
				if ((char) bytes[start+4] == CycleCommand.TOKEN_SEPARATOR_CHAR) return null;
				
				String pid = new String(bytes, start+1, 2);
				
				/*
				 * METADATA Stuff
				 */
				if (pid.equals("14")) {
					logger.debug("Status: CONNECTING");
				}
				else if (pid.equals("15")) {
					processVIN(new String(bytes, start+3, count-3));
				}
				else if (pid.equals("70")) {
					/*
					 * short term fix for #192: disable
					 */
//					processSupportedPID(bytes, start, count);
				}
				else if (pid.equals("71")) {
					processDiscoveredControlUnits(new String(bytes, start+3, count-3));
				}
				
				else {
					/*
					 * A PID response
					 */
					long now = System.currentTimeMillis();
					logger.info("Processing PID Response:" +pid);
					
					byte[] pidResponseValue = new byte[2];
					int target;
					for (int i = start+4; i <= count+start; i++) {
						if ((char) bytes[i] == CycleCommand.TOKEN_SEPARATOR_CHAR)
							break;
						
						target = i-(start+4);
						if (target >= pidResponseValue.length) break;
						pidResponseValue[target] = bytes[i];
					}
					
					return parsePIDResponse(pid, pidResponseValue, now);
				}
				
			}
			else if (type == 'C') {
				determineProtocol(new String(bytes, start+1, count-1));
			}
			
			return null;
		}

		@Override
		public char getEndOfLine() {
			return END_OF_LINE_RESPONSE;
		}


	}


	@Override
	protected long getSleepTimeBetweenCommands() {
		return 0;
	}

}
