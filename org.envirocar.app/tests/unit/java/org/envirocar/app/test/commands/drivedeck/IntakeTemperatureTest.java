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
package org.envirocar.app.test.commands.drivedeck;

import android.os.Environment;
import android.util.Base64;

import junit.framework.Assert;

import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.app.logging.Handler;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.protocol.ResponseParser;
import org.envirocar.app.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class, Logger.class, Base64.class})
public class IntakeTemperatureTest extends CommandTest {


	@Test
	public void testParsing() throws IOException {
		ResponseParser parser = getResponseParser();
		
		byte[] bytes = createBytes();
		CommonCommand resp = parser.processResponse(bytes, 0, bytes.length);
		
		Assert.assertTrue(resp != null && resp instanceof IntakeTemperature);
		
		double temp = ((IntakeTemperature) resp).getNumberResult().doubleValue();
		Assert.assertTrue(temp == 23.0);
	}

	private byte[] createBytes() {
		return "B52<?0".getBytes();
	}


}
