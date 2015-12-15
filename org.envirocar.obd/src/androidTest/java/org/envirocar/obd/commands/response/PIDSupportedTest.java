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

        Set<PID> pids = pidSupported.parsePIDs("BE1FA813".getBytes());

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
    }

}
