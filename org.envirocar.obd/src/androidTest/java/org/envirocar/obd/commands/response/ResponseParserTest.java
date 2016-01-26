package org.envirocar.obd.commands.response;

import android.test.InstrumentationTestCase;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ResponseParserTest extends InstrumentationTestCase {

    @Test
    public void testLambdaParsing() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        ResponseParser responseParser = new ResponseParser();

        DataResponse response = responseParser.parse("412407FF0028".getBytes());

        Assert.assertThat(response.getPid(), CoreMatchers.is(PID.O2_LAMBDA_PROBE_1_VOLTAGE));
        Assert.assertThat(response.isComposite(), CoreMatchers.is(true));

        Number[] composites = response.getCompositeValues();

        Assert.assertThat(composites.length, CoreMatchers.is(2));

        //ER: byte A = 7, byte B = 255 --> ((7*256)+255)*2/65535
        Assert.assertThat(composites[0], CoreMatchers.is(((7*256)+255)/32768d));
        //Voltage: byte C = 0, byte D = 40--> ((0*256)+40)*8/65535
        Assert.assertThat(composites[1], CoreMatchers.is(((0*256)+40)/8192d));
    }

    public void testLambdaSwitched() throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        ResponseParser responseParser = new ResponseParser();

        DataResponse parse = responseParser.parse("41241DBC3B48".getBytes());

        DataResponse parseSwitch = responseParser.parse("41243B481DBC".getBytes());

        Assert.assertThat(parse, CoreMatchers.instanceOf(LambdaProbeVoltageResponse.class));
    }

}
