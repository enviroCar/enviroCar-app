/*
 * Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 * 
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.envirocar.app.logging;

import java.util.logging.FileHandler;

public class LocalFileHandler implements Handler {

	private java.util.logging.Logger logger;

	public LocalFileHandler(String localLogFile) throws Exception {
		this.logger = java.util.logging.Logger.getLogger("org.envirocar.app");
		this.logger.addHandler(new FileHandler(localLogFile));
	}

	@Override
	public void logMessage(int level, String msg) {
		switch (level) {
		case Logger.SEVERE:
			this.logger.severe(msg);
			break;
		case Logger.WARNING:
			this.logger.warning(msg);
			break;
		case Logger.INFO:
			this.logger.info(msg);
			break;
		case Logger.FINE:
			this.logger.fine(msg);
			break;
		case Logger.VERBOSE:
			this.logger.finer(msg);
			break;
		case Logger.DEBUG:
			this.logger.finest(msg);
			break;
		default:
			this.logger.info(msg);
			break;
		}
	}

}
