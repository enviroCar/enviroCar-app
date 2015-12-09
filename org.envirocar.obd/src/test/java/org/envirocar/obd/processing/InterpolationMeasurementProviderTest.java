package org.envirocar.obd.processing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class InterpolationMeasurementProviderTest {

    @Test
    public void testInterpolateTwo() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider();

        DataResponse s1 = new MockResponse(PID.SPEED, 52, 1000);
        DataResponse s2 = new MockResponse(PID.SPEED, 95, 4000);

        //the temporal center, should be the average
        double result = imp.interpolateTwo(s1.getValue(), s2.getValue(), 2500, s1.getTimestamp(), s2.getTimestamp());

        Assert.assertThat(result, CoreMatchers.is(73.5));

        //more at the and of the window
        result = imp.interpolateTwo(s1.getValue(), s2.getValue(), 3000, s1.getTimestamp(), s2.getTimestamp());

        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(2, RoundingMode.HALF_UP);

        Assert.assertThat(bd.doubleValue(), CoreMatchers.is(80.67));
    }

    @Test
    public void testInterpolation() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider();

        DataResponse m1 = new MockResponse(PID.MAF, 16.0, 1000);
        DataResponse m2 = new MockResponse(PID.MAF, 48.0, 3500); // this should be the result
        DataResponse m3 = new MockResponse(PID.MAF, 32.0, 5000);

        //the result should be 68.125
        DataResponse s1 = new MockResponse(PID.SPEED, 52, 2000);
        DataResponse s2 = new MockResponse(PID.SPEED, 95, 6000);

        imp.consider(s1);
        imp.consider(s2);
        imp.consider(m1);
        imp.consider(m2);
        imp.consider(m3);

        TestSubscriber<Measurement> ts = new TestSubscriber<Measurement>();
        
        imp.measurements(500)
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .first()
                .subscribe(ts);

        List<Measurement> events = ts.getOnNextEvents();
        Assert.assertThat(events.size(), CoreMatchers.is(1));

        Assert.assertThat(events.get(0).getTime(), CoreMatchers.is(3500L));

        Assert.assertThat(events.get(0).getProperty(Measurement.PropertyKey.MAF), CoreMatchers.is(48.0));
        Assert.assertThat(events.get(0).getProperty(Measurement.PropertyKey.SPEED), CoreMatchers.is(68.125));
    }

    @Test
    public void testInterpolateLambdaComposites() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider();

        DataResponse s1 = new MockLambdaVoltageResponse(1.1, 1.96, 1000);
        DataResponse s2 = new MockLambdaVoltageResponse(1.0, 1.12, 2000);

        imp.consider(s1);
        imp.consider(s2);

        TestSubscriber<Measurement> ts = new TestSubscriber<Measurement>();

        imp.measurements(500)
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .first()
                .subscribe(ts);

        List<Measurement> events = ts.getOnNextEvents();
        Assert.assertThat(events.size(), CoreMatchers.is(1));

        Assert.assertThat(events.get(0).getTime(), CoreMatchers.is(1500L));

        Assert.assertThat(events.get(0).getProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE), CoreMatchers.is(1.05));
        Assert.assertThat(events.get(0).getProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER), CoreMatchers.is(1.54));
    }

    private class MockResponse extends DataResponse {

        private final long timestamp;
        private final PID pi;
        private final Number value;

        public MockResponse(PID pi, Number value, long ts) {
            this.pi = pi;
            this.value = value;
            this.timestamp = ts;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public PID getPid() {
            return pi;
        }

        @Override
        public Number getValue() {
            return value;
        }
    }

    private class MockLambdaVoltageResponse extends LambdaProbeVoltageResponse {

        private final long ts;

        public MockLambdaVoltageResponse(double voltage, double er, long timestamp) {
            super(voltage, er);
            this.ts = timestamp;
        }

        @Override
        public long getTimestamp() {
            return ts;
        }
    }
}
