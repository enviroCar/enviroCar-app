/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.protocol;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.protocol.exception.LooperStoppedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AsynchronousResponseThread extends HandlerThread {
	
	private static final Logger logger = Logger.getLogger(AsynchronousResponseThread.class);
	private static final int MAX_BUFFER_SIZE = 32;
	private Handler handler;
	private InputStream inputStream;
	
	private Runnable readInputStreamRunnable;
	
	private List<CommonCommand> buffer = new ArrayList<CommonCommand>();
	
	protected boolean running = true;
	private byte[] globalBuffer = new byte[64];
	private int globalIndex;
	private ResponseParser responseParser;

	public AsynchronousResponseThread(final InputStream in, ResponseParser responseParser) {
		super("AsynchronousResponseThread");
		this.inputStream = in;
		
		this.responseParser = responseParser;
		
		this.readInputStreamRunnable = new Runnable() {
			
			@Override
			public void run() {
				while (running) {
					
					CommonCommand cmd;
					try {
						cmd = readResponse();	
						
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
	
	private CommonCommand readResponse() throws IOException {
		byte byteIn;
		int intIn;
		while (running) {
			intIn = inputStream.read();
			
			if (intIn < 0) {
				break;
			}

			byteIn = (byte) intIn;
			
			if (byteIn == (byte) responseParser.getEndOfLine()) {
				boolean isReplete = false;
				synchronized (this) {
					/*
					 * are we fed?
					 */
					isReplete = buffer.size() > MAX_BUFFER_SIZE;
				}
				
				CommonCommand result = null;
				if (!isReplete) {
					result = responseParser.processResponse(globalBuffer,
							0, globalIndex);	
				}
				
				globalIndex = 0;
				return result;
			} else {
				globalBuffer[globalIndex++] = byteIn;
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

	public void shutdown() {
		logger.info("SHUTDOWN!");
		running = false;
	}

	public boolean isRunning() {
		return running;
	}
	
	
}
