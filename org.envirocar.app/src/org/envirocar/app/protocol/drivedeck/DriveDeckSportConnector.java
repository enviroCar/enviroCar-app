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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.OBDConnector;
import org.envirocar.app.protocol.drivedeck.CycleCommand.PID;
import org.envirocar.app.protocol.exception.AdapterFailedException;

public class DriveDeckSportConnector implements OBDConnector {

	private static final Logger logger = Logger.getLogger(DriveDeckSportConnector.class);
	private static final char CARRIAGE_RETURN = '\r';
	static final char END_OF_LINE_RESPONSE = '>';
	static final String TOKEN_DELIMITER_RESPONSE = "<";
	static final int SLEEP_TIME = 25;
	static final int TIMEOUT = 1000 * 5;
	private static final long MAX_WAITING_TIME = 1000 * 30;
	private Mode mode = Mode.OFFLINE;
	private Protocol protocol;
	private String vin;
	private long firstConnectinResponse;
	private CycleCommand cycleCommand;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Object inputMutex;
	private AsynchronousResponseThread responseThread;
	private Object outputMutex;
	private boolean send;
	
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
	
	
	private void waitForInitialResponses() throws IOException {
		logger.info("waiting for initial responses...");
		
		waitForResponse();	
		
		logger.info("Reading response...");
		
		byte[] buffer = new byte[1024];
		int index = 0;
		int i;
		while ((i = inputStream.read()) > 0 && !hasReceivedAllMetadata()) {
			buffer[index++] = (byte) i; 
			if (i == END_OF_LINE_RESPONSE) {
				String response = new String(buffer, 0, index-1);
				if (!processMetadataResponse(response)) return;
				index = 0;
			}
		}
		
		logger.info("Connection established!");
	}


	protected void waitForResponse() throws IOException {
		try {
			int tries = 0;
			while (inputStream.available() <= 0) {
				if (tries++ * SLEEP_TIME > TIMEOUT) {
					logger.info("could not receive anything from DriveDeck adapter.");
					return;
				}
				
				Thread.sleep(SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private void createCycleCommand() {
		List<PID> pidList = new ArrayList<PID>();
		pidList.add(PID.SPEED);
//		pidList.add(PID.MAF);
//		pidList.add(PID.RPM);
//		pidList.add(PID.IAP);
//		pidList.add(PID.IAT);
		this.cycleCommand = new CycleCommand(pidList);
	}

	private boolean hasReceivedAllMetadata() {
		return protocol != null && vin != null;
	}

	private boolean processMetadataResponse(String response) {
		logger.info("Received result: "+ response);
		
		response = response.trim();
		
		if (response.equals("B14")) {
			mode = Mode.CONNECTING;
			if (firstConnectinResponse == 0) {
				firstConnectinResponse = System.currentTimeMillis();
			} else {
				if (System.currentTimeMillis() - firstConnectinResponse > MAX_WAITING_TIME) {
					logger.info("We have been waiting to long for a meaningful response.");
					return false;
				}
			}
			logger.info("Mode: "+ mode.toString());
		}
		else if (response.startsWith("C")) {
			determineProtocol(response.substring(1));
		}
		else if (response.startsWith("B15")) {
			processVIN(response.substring(3));
		}
		else if (response.startsWith("B70")) {
			processSupportedPID(response.substring(3));
		}
		else if (response.startsWith("B71")) {
			processDiscoveredControlUnits(response.substring(3));
		}
		
		return true;
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


	@Override
	public boolean connectionVerified() {
		if (hasReceivedAllMetadata()) {
			startResponseThread();
			return true;
		}
		return false;
	}

	private void startResponseThread() {
		if (responseThread == null) {
			responseThread = new AsynchronousResponseThread(inputStream, inputMutex);
			responseThread.start();
		}
	}


	@Override
	public void provideStreamObjects(InputStream inputStream,
			OutputStream outputStream, Object inputMutex, Object outputMutex) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.inputMutex = inputMutex;
		this.outputMutex = outputMutex;
	}

	@Override
	public void executeInitializationCommands() throws IOException,
			AdapterFailedException {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
		
		synchronized (outputMutex) {
			logger.info("Sending out initial CR.");
			outputStream.write(CARRIAGE_RETURN);
			outputStream.flush();
			
			waitForInitialResponses();	
		}
	}

	@Override
	public List<CommonCommand> executeRequestCommands() throws IOException,
			AdapterFailedException {
		synchronized (outputMutex) {
			if (!send) {
				logger.info("Sending CycleCommand: "+new String(cycleCommand.getOutgoingBytes()));
				
				outputStream.write(cycleCommand.getOutgoingBytes());
				outputStream.write(CARRIAGE_RETURN);
				outputStream.flush();
				send = true;
			}
		}
		
		return responseThread.pullAvailableCommands();
	}



	@Override
	public void shutdown() {
		if (responseThread != null) {
			responseThread.setRunning(false);
		}
	}



}
