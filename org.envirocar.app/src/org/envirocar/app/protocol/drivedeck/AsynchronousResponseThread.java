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
import java.util.ArrayList;
import java.util.List;

import org.envirocar.app.commands.CommonCommand;
import org.envirocar.app.commands.IntakePressure;
import org.envirocar.app.commands.IntakeTemperature;
import org.envirocar.app.commands.MAF;
import org.envirocar.app.commands.RPM;
import org.envirocar.app.commands.Speed;
import org.envirocar.app.commands.CommonCommand.CommonCommandState;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.exception.LooperStoppedException;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class AsynchronousResponseThread extends HandlerThread {
	
	private static final Logger logger = Logger.getLogger(AsynchronousResponseThread.class);
	private Handler handler;
	private InputStream inputStream;
	
	private Runnable readInputStreamRunnable;
	private Object socketMutex;
	
	private List<CommonCommand> buffer = new ArrayList<CommonCommand>();
	protected boolean running = true;
	private int[] globalBuffer = new int[128];
	private int globalIndex;

	public AsynchronousResponseThread(final InputStream in, Object sm) {
		super("AsynchronousResponseThread");
		this.inputStream = in;
		this.socketMutex = sm;
		
		this.readInputStreamRunnable = new Runnable() {
			
			@Override
			public void run() {
				while (running) {
					
					try {
						synchronized (socketMutex) {
							waitForResponse();
						}
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
					
					CommonCommand cmd;
					try {
						while (true) {
							synchronized (socketMutex) {
								cmd = readResponse();	
							}
							
							if (cmd == null) break;
							
							synchronized (AsynchronousResponseThread.this) {
								buffer.add(cmd);	
							}
						}
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
				}
				
				throw new LooperStoppedException();
			}
		};
	}
	
	protected void waitForResponse() throws IOException {
		try {
			int tries = 0;
			while (inputStream.available() <= 0) {
				if (tries++ * DriveDeckSportConnector.SLEEP_TIME > DriveDeckSportConnector.TIMEOUT) {
					logger.info("could not receive anything from DriveDeck adapter.");
					return;
				}
				
				Thread.sleep(DriveDeckSportConnector.SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	private CommonCommand readResponse() throws IOException {
		int i;
		int index = 0;
		byte[] buffer = new byte[128];
		while ((i = inputStream.read()) > 0) {
			buffer[index++] = (byte) i; 
			globalBuffer[globalIndex++] = i;
			
			if (globalIndex % 128 == 0) {
				logGlobalBuffer(globalIndex);
				if (globalIndex >= globalBuffer.length)
					globalIndex = 0;
			}
			
			if (i == DriveDeckSportConnector.END_OF_LINE_RESPONSE) {
				String response = new String(buffer, 0, index-1);
				
				return processResponse(response);
			}
		}
		
		return null;
	}
	
	private void logGlobalBuffer(int limit) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for (int i : globalBuffer) {
			if (i > limit) break;
			sb.append(i);
			sb.append(", ");
		}
		sb.append(" ]");
		logger.info(sb.toString());
	}

	private CommonCommand processResponse(String response) {
		if (!response.startsWith("B")) {
			logger.warn("This is not a CycleCommand response: "+response);
			return null;
		}
		
		logger.info("Processing CycleCommand response: "+response);
		String[] tokens = response.substring(1).split(DriveDeckSportConnector.TOKEN_DELIMITER_RESPONSE);
		if (tokens != null && tokens.length > 1) {
			String pp = tokens[0];
			String xx = tokens[1];
			return handlePIDResponse(pp, xx);
		}
		
		return null;
	}


	private CommonCommand handlePIDResponse(String pp, String xx) {
		long now = System.currentTimeMillis();
		CommonCommand result = null;
		String rawData = pp.concat(xx);
		if (pp.equals("41")) {
			//Speed
			result = new Speed();
		}
		else if (pp.equals("42")) {
			//MAF
			result = new MAF();
		}
		else if (pp.equals("52")) {
			//IAP
			result = new IntakeTemperature();
		}
		else if (pp.equals("49")) {
			//IAT
			result = new IntakePressure();
		}
		else if (pp.equals("40") || pp.equals("51")) {
			//RPM
			result = new RPM();
		}
		
		if (result != null) {
			result.setRawData(rawData);
			result.parseRawData();
			result.setCommandState(CommonCommandState.FINISHED);
			result.setResultTime(now);
		}
		
		return result;
	}

	@Override
	public void run() {
		Looper.prepare();
		handler = new Handler();
		handler.post(readInputStreamRunnable);
		try {
			Looper.loop();
		} catch (LooperStoppedException e) {
			logger.info("AsynchronousResponseThread stopped.");
		}
	}

	public List<CommonCommand> pullAvailableCommands() {
		List<CommonCommand> result;
		synchronized (this) {
			result = new ArrayList<CommonCommand>(buffer.size());
			result.addAll(buffer);
			buffer.clear();
		}
		return result;
	}

	public void setRunning(boolean b) {
		running = false;
	}
	
}
