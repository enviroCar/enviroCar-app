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
package org.envirocar.obd.adapter.async;

import android.util.Base64;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.EngineRPMResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class DriveDeckParserTest {

    @Test
    public void testSpeedParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        byte[] bytes = new byte[]{66, 52, 49, 60, (byte) 77, (byte) 0, 60, 0, 0, 32, 32};

        DriveDeckSportAdapter dd = new DriveDeckSportAdapter();

        DataResponse resp = dd.processResponse(bytes);

        Assert.assertThat(resp.getPid(), CoreMatchers.is(PID.SPEED));
        Assert.assertThat(resp.getValue(), CoreMatchers.is(77));
    }

    @Test
    public void testLambdaParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        byte[] decode = Base64.decode("QjREPH+dPAAAPFzy", Base64.DEFAULT);

        DriveDeckSportAdapter dd = new DriveDeckSportAdapter();

        DataResponse resp = dd.processResponse(decode);

        Assert.assertThat(resp, CoreMatchers.instanceOf(LambdaProbeVoltageResponse.class));
    }

    @Test
    public void testPIDSupportedParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        byte[] decode = Base64.decode("QjcwN0U4MDA8mDs8oBM=", Base64.DEFAULT);

        DriveDeckSportAdapter driveDeckSportAdapter = new DriveDeckSportAdapter();

        driveDeckSportAdapter.processResponse(decode);

        Assert.assertThat(driveDeckSportAdapter.getSupportedPIDs().size(), CoreMatchers.not(0));
    }

    @Test
    public void testRPMSpecialCaseParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        byte[] decode = Base64.decode("QjUxPAAQPAwYPAAAPF0u", Base64.DEFAULT);

        DriveDeckSportAdapter dd = new DriveDeckSportAdapter();

        DataResponse resp = dd.processResponse(decode);

        Assert.assertThat(resp, CoreMatchers.instanceOf(EngineRPMResponse.class));
        Assert.assertThat(resp.getValue(), CoreMatchers.is(774));
    }

}
