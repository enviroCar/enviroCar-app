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
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.ResponseParser;
import org.envirocar.app.protocol.exception.LooperStoppedException;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class AsynchronousResponseThread extends HandlerThread {
	
	private static final Logger logger = Logger.getLogger(AsynchronousResponseThread.class);
	private static final long SLEEP_TIME = 25;
	private Handler handler;
	private InputStream inputStream;
	
	private Runnable readInputStreamRunnable;
	private Object socketMutex;
	
	private List<CommonCommand> buffer = new ArrayList<CommonCommand>();
	protected boolean running = true;
	private byte[] globalBuffer = new byte[64];
	private int globalIndex;
	private int previousEOLIndex = -1;
	private ResponseParser responseParser;

	public AsynchronousResponseThread(final InputStream in, Object sm, ResponseParser responseParser) {
		super("AsynchronousResponseThread");
		this.inputStream = in;
		this.socketMutex = sm;
		
		this.responseParser = responseParser;
		
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
				Thread.sleep(SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	private CommonCommand readResponse() throws IOException {
		byte byteIn;
		while (true) {
			byteIn = (byte) inputStream.read();
			
			if ((int) byteIn <= 0) {
				break;
			}
			
			globalBuffer[globalIndex++] = byteIn;
			
			if (byteIn == (byte) responseParser.getEndOfLine()) {
				if (previousEOLIndex != -1) {
					CommonCommand result = responseParser.processResponse(globalBuffer,
							previousEOLIndex, globalIndex);
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
