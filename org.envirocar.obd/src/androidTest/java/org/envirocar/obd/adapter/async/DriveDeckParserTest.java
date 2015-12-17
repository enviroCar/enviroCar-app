package org.envirocar.obd.adapter.async;

import android.test.InstrumentationTestCase;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class DriveDeckParserTest extends InstrumentationTestCase {

    @Test
    public void testSpeedParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        byte[] bytes = new byte[]{66, 52, 49, 60, (byte) 77, (byte) 0, 60, 0, 0, 32, 32};

        DriveDeckSportAdapter dd = new DriveDeckSportAdapter();

        DataResponse resp = dd.processResponse(bytes);

        Assert.assertThat(resp.getPid(), CoreMatchers.is(PID.SPEED));
        Assert.assertThat(resp.getValue(), CoreMatchers.is(77));
    }

//    @Test
//    public void testPIDSupportedParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
//        byte[] decode = Base64.decode("QjcwN0U4MDA8vg==", Base64.DEFAULT);
//
//        DriveDeckSportAdapter dd = new DriveDeckSportAdapter();
//
//        dd.processSupportedPID(decode);
//    }

}
