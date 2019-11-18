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
package org.envirocar.obd.adapter.async;

import android.test.InstrumentationTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CyclicCommandTest extends InstrumentationTestCase {

    @Test
    public void testBytes() {
        List<CycleCommand.DriveDeckPID> pidList = new ArrayList<>();

        pidList.add(CycleCommand.DriveDeckPID.SPEED);
        pidList.add(CycleCommand.DriveDeckPID.RPM);
        pidList.add(CycleCommand.DriveDeckPID.IAP);
        pidList.add(CycleCommand.DriveDeckPID.IAT);

        CycleCommand cycleCommand = new CycleCommand(pidList);

        byte[] expected = new byte[] {97, 49, 55, 26, 25, 24, 28};

        Assert.assertThat(expected, CoreMatchers.equalTo(cycleCommand.getOutputBytes()));
    }

}
