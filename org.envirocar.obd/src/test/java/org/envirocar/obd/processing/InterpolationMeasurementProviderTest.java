package org.envirocar.obd.processing;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class InterpolationMeasurementProviderTest {

    @Test
    public void testInterpolateTwo() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider();

        DataResponse s1 = new DataResponse() {
            @Override
            public long getTimestamp() {
                return 1000;
            }

            @Override
            public Number getValue() {
                return 52;
            }

            @Override
            public PID getPid() {
                return PID.SPEED;
            }
        };

        DataResponse s2 = new DataResponse() {
            @Override
            public long getTimestamp() {
                return 4000;
            }

            @Override
            public Number getValue() {
                return 95;
            }

            @Override
            public PID getPid() {
                return PID.SPEED;
            }
        };

        //the temporal center, should be the average
        double result = imp.interpolateTwo(s1, s2, 2500);

        Assert.assertThat(result, CoreMatchers.is(73.5));

        //more at the and of the window
        result = imp.interpolateTwo(s1, s2, 3000);

        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(2, RoundingMode.HALF_UP);

        Assert.assertThat(bd.doubleValue(), CoreMatchers.is(80.67));
    }
}
