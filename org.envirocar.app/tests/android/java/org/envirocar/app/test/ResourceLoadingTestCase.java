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
package org.envirocar.app.test;

import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class ResourceLoadingTestCase extends InstrumentationTestCase {

	protected String readJson(InputStream in) {
		Scanner sc = new Scanner(in, "UTF-8");
		
		StringBuilder sb = new StringBuilder();
		while (sc.hasNext()) {
			sb.append(sc.nextLine());
			sb.append(System.getProperty("line.separator"));
		}
		
		sc.close();
		return sb.toString();
	}
	
	protected String readJsonAsset(String assetRes) throws IOException {
		return readJson(getClass().getResourceAsStream(assetRes));
	}
	
}
