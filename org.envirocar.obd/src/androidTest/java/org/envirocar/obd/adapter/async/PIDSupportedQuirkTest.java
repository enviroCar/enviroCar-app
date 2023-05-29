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

import org.envirocar.obd.adapter.CommandExecutor;
import org.envirocar.obd.exception.StreamFinishedException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class PIDSupportedQuirkTest {

    @Test
    public void testQuirk() {
        byte[] bytesWait = Base64.decode("QjcwN0U4MDA8vg==", Base64.DEFAULT);
        byte[] bytesFull = Base64.decode("QjcwN0U4MDA8vj48uBM=", Base64.DEFAULT);

        PIDSupportedQuirk quirk = new PIDSupportedQuirk();
        Assert.assertTrue(quirk.shouldWaitForNextTokenLine(bytesWait));
        Assert.assertFalse(quirk.shouldWaitForNextTokenLine(bytesFull));
    }

    @Test
    public void testQuirkIntegration() throws IOException, StreamFinishedException {
        byte[] bytesFull = Base64.decode("QjcwN0U4MDA8vj48uBM=", Base64.DEFAULT);
        byte[] bytesFullWithEnd = Arrays.copyOf(bytesFull, bytesFull.length+1);
        bytesFullWithEnd[bytesFullWithEnd.length-1] = DriveDeckSportAdapter.END_OF_LINE_RESPONSE;

        PIDSupportedQuirk quirk = new PIDSupportedQuirk();

        CommandExecutor executor = new CommandExecutor(new ByteArrayInputStream(bytesFullWithEnd), new ByteArrayOutputStream(),
                Collections.emptySet(),
                DriveDeckSportAdapter.END_OF_LINE_RESPONSE, DriveDeckSportAdapter.CARRIAGE_RETURN);
        executor.setQuirk(quirk);

        byte[] bytes = executor.retrieveLatestResponse();

        Assert.assertTrue(Arrays.equals(bytes, bytesFull));
    }

}
