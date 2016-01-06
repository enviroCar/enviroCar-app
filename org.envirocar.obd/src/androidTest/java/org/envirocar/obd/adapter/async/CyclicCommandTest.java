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
