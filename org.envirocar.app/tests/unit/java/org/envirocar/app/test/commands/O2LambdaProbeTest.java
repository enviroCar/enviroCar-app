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
package org.envirocar.app.test.commands;

import java.math.BigDecimal;

import junit.framework.Assert;

import org.envirocar.obd.commands.O2LambdaProbe;
import org.envirocar.obd.commands.O2LambdaProbeCurrent;
import org.envirocar.obd.commands.O2LambdaProbeVoltage;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.PIDUtil.PID;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.test.MockingEnvironmentTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import android.os.Environment;
import android.util.Base64;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class, Logger.class, Base64.class})
public class O2LambdaProbeTest extends MockingEnvironmentTest{

	@Test
	public void testVoltageParsing() {
		O2LambdaProbeVoltage cmd = (O2LambdaProbeVoltage) PIDUtil.instantiateCommand(PID.O2_LAMBDA_PROBE_1_VOLTAGE);
		
		cmd.setRawData(createDataVoltage());
		cmd.parseRawData();
		
		BigDecimal er = BigDecimal.valueOf(cmd.getEquivalenceRatio()).setScale(2, BigDecimal.ROUND_HALF_UP);
		Assert.assertTrue("Expected equivalence ration of 1.52.", er.doubleValue() == 1.52);
		
		BigDecimal v = BigDecimal.valueOf(cmd.getVoltage()).setScale(2, BigDecimal.ROUND_HALF_UP);
		Assert.assertTrue("Expected voltage of 7.5.", v.doubleValue() == 6.08);
	}

    @Test
	public void testCurrentParsing() {
		O2LambdaProbeCurrent cmd = (O2LambdaProbeCurrent) O2LambdaProbe.fromPIDEnum(PID.O2_LAMBDA_PROBE_1_CURRENT);
		
		cmd.setRawData(createDataCurrent());
		cmd.parseRawData();
		
		BigDecimal er = BigDecimal.valueOf(cmd.getEquivalenceRatio()).setScale(2, BigDecimal.ROUND_HALF_UP);
		Assert.assertTrue("Expected equivalence ration of 1.52.", er.doubleValue() == 1.52);
		
		BigDecimal c = BigDecimal.valueOf(cmd.getCurrent()).setScale(2, BigDecimal.ROUND_HALF_UP);
		Assert.assertTrue("Expected current of 128.", c.doubleValue() == 2.5);
	}

	private byte[] createDataCurrent() {
		return "4134C2908280".getBytes();
	}

	private byte[] createDataVoltage() {
		return "4124C290C290".getBytes();
	}

}
