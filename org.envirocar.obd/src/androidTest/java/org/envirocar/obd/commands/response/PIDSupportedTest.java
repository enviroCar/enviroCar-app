/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.obd.commands.response;

import android.test.InstrumentationTestCase;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDSupported;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PIDSupportedTest extends InstrumentationTestCase {

    @Test
    public void testParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        PIDSupported pidSupported = new PIDSupported();

        Set<PID> pids = pidSupported.parsePIDs("4100BE1FA813".getBytes());

        Assert.assertThat(pids, CoreMatchers.hasItems(
                PID.FUEL_SYSTEM_STATUS,
                PID.CALCULATED_ENGINE_LOAD,
                PID.SHORT_TERM_FUEL_TRIM_BANK_1,
                PID.LONG_TERM_FUEL_TRIM_BANK_1,
                PID.RPM,
                PID.SPEED,
                PID.INTAKE_AIR_TEMP,
                PID.MAF,
                PID.TPS
                ));
        Assert.assertThat(pids, CoreMatchers.not(CoreMatchers.hasItems(
                PID.INTAKE_MAP
        )));

        pidSupported = new PIDSupported("20");

        pids = pidSupported.parsePIDs("41201A090F01".getBytes());

        Assert.assertThat(pids, CoreMatchers.hasItems(
                PID.O2_LAMBDA_PROBE_1_VOLTAGE,
                PID.O2_LAMBDA_PROBE_2_VOLTAGE,
                PID.O2_LAMBDA_PROBE_4_VOLTAGE,
                PID.O2_LAMBDA_PROBE_2_CURRENT,
                PID.O2_LAMBDA_PROBE_3_CURRENT,
                PID.O2_LAMBDA_PROBE_4_CURRENT,
                PID.O2_LAMBDA_PROBE_5_CURRENT
        ));
        Assert.assertThat(pids, CoreMatchers.not(CoreMatchers.hasItems(
                PID.O2_LAMBDA_PROBE_3_VOLTAGE,
                PID.O2_LAMBDA_PROBE_5_VOLTAGE,
                PID.O2_LAMBDA_PROBE_6_VOLTAGE,
                PID.O2_LAMBDA_PROBE_7_VOLTAGE,
                PID.O2_LAMBDA_PROBE_8_VOLTAGE,
                PID.O2_LAMBDA_PROBE_1_CURRENT,
                PID.O2_LAMBDA_PROBE_6_CURRENT,
                PID.O2_LAMBDA_PROBE_7_CURRENT,
                PID.O2_LAMBDA_PROBE_8_CURRENT
        )));
    }

    @Test
    public void testMalformedResponses() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        PIDSupported pidSupported = new PIDSupported();
        Set<PID> pids = pidSupported.parsePIDs("SEARCHING...4100BE3EB813".getBytes());

        Assert.assertThat(pids, CoreMatchers.hasItems(PID.SPEED));
    }

}
