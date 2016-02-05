package org.envirocar.obd.adapter.async;

import android.test.InstrumentationTestCase;
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

public class PIDSupportedQuirkTest extends InstrumentationTestCase {

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
