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
import org.envirocar.app.commands.RPM;
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
	private int[] globalBuffer = new int[64];
	private int globalIndex;
	private int previousEOLIndex = -1;

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
						break;
					}
					
					CommonCommand cmd;
					try {
						synchronized (socketMutex) {
							cmd = readResponse();	
						}
						
						if (cmd != null) {
							synchronized (AsynchronousResponseThread.this) {
								buffer.add(cmd);	
							}	
						}
						
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
						break;
					}
				}
				
				throw new LooperStoppedException();
			}
		};
	}
	
	protected void waitForResponse() throws IOException {
		try {
			while (inputStream.available() <= 0) {
				Thread.sleep(DriveDeckSportConnector.SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	private CommonCommand readResponse() throws IOException {
		int byteIn;
		while (true) {
			byteIn = inputStream.read();
			if (byteIn <= 0) {
				break;
			}
			globalBuffer[globalIndex++] = byteIn;
			
			if (byteIn == DriveDeckSportConnector.END_OF_LINE_RESPONSE) {
				if (previousEOLIndex != -1) {
					CommonCommand result = processResponse(globalBuffer, previousEOLIndex, globalIndex);
					previousEOLIndex = 0;
					globalIndex = 0;
					return result;
				} else {
					previousEOLIndex = globalIndex;
				}
			}
		}
		
		return null;
	}
	
	private CommonCommand processResponse(int[] bytes, int start, int end) {
		if (start >= end) return null;
		
		logGlobalBuffer(end);
		
		if (bytes[start+0] != CycleCommand.RESPONSE_PREFIX_CHAR_AS_INT) return null;
		
		if (bytes[start+4] == CycleCommand.TOKEN_SEPARATOR_CHAR_AS_INT) return null;
		
		long now = System.currentTimeMillis();
		
		String pid = new String(bytes, start+1, 2);
		
		int[] pidResponseValue = new int[2];
		int target;
		for (int i = start+4; i <= end; i++) {
			if (bytes[i] == CycleCommand.TOKEN_SEPARATOR_CHAR_AS_INT)
				break;
			
			target = i-start-4;
			if (target >= pidResponseValue.length) break;
			pidResponseValue[target] = bytes[i];
		}
		
		return parseCommandReponse(pid, pidResponseValue, now);
	}

	private CommonCommand parseCommandReponse(String pid,
			int[] pidResponseValue, long now) {
		
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
			result.setResponseBytes(pidResponseValue);
			result.parseRawData();
			result.setCommandState(CommonCommandState.FINISHED);
			result.setResultTime(now);
		}
		
		return result;
	}

	private void logGlobalBuffer(int limit) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		
		byte[] bytes = new byte[limit];
		
		int c = 0;
		for (int i : globalBuffer) {
			bytes[c++] = (byte) i;
			if (c >= limit) break;
			sb.append(i);
			sb.append(", ");
		}
		sb.append(" ]");
		logger.info(sb.toString());
		logger.info(new String(bytes));
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
