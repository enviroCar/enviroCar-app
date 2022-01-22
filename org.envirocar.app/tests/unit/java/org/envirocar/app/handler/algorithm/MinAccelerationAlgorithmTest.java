package org.envirocar.app.handler.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MinAccelerationAlgorithmTest {

    private MinAccelerationAlgorithm algorithm;

    @Before
    public void setup(){
        algorithm = new MinAccelerationAlgorithm();
    }

    @Test
    public void testCalculateForLessThenTwoValues(){
        PropertyKeyEvent m1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 16.0, 1000);
        List<PropertyKeyEvent> pkes = Arrays.asList(m1);

        Assert.assertNull(algorithm.calculate(pkes));
    }

    @Test
    public void test(){
        PropertyKeyEvent m1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 16.0, 1000);
        PropertyKeyEvent m2 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 45.0, 1300);
        PropertyKeyEvent m3 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 61.2, 1900);
        PropertyKeyEvent m4 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 126.0, 2300);
        PropertyKeyEvent m5 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, null, 2900);

        List<PropertyKeyEvent> pkes = Arrays.asList(m1, m2, m3, m4, m5);

        Assert.assertEquals(7.5, algorithm.calculate(pkes).doubleValue(),0.0001);
    }

}
