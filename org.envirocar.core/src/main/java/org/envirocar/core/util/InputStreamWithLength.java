/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.core.util;

import java.io.InputStream;

public class InputStreamWithLength {

	private InputStream inputStream;
	private long length;
	private boolean gzipped;
	
	public InputStreamWithLength(InputStream inputStream, long length, boolean gzip) {
		this.inputStream = inputStream;
		this.gzipped = gzip;
		this.length = length;
	}

	public long getLength() {
		return this.length;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public boolean isGzipped() {
		return gzipped;
	}

}
