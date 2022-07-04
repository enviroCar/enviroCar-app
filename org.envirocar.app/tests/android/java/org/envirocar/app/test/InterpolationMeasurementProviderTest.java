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
package org.envirocar.app.test;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.handler.InterpolationMeasurementProvider;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class InterpolationMeasurementProviderTest {

    @Test
    public void testInterpolateTwo() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider();

        PropertyKeyEvent s1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 52, 1000);
        PropertyKeyEvent s2 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 95, 4000);

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

        PropertyKeyEvent m1 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 16.0, 1000);
        PropertyKeyEvent m2 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 48.0, 3500); // this should be the result
        PropertyKeyEvent m3 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 32.0, 5000);

        //the result should be 68.125
        PropertyKeyEvent s1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 52, 2000);
        PropertyKeyEvent s2 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 95, 6000);

        imp.consider(s1);
        imp.consider(s2);
        imp.consider(m1);
        imp.consider(m2);
        imp.consider(m3);

        imp.newPosition(new MeasurementProvider.Position(1000, 52.0, 7.0));
        imp.newPosition(new MeasurementProvider.Position(3500, 52.5, 7.25)); //this should be the result

        TestObserver<Measurement> ts = new TestObserver<Measurement>();

        imp.measurements(500)
                .subscribeOn(Schedulers.trampoline())
                .observeOn(Schedulers.trampoline())
                .subscribe(ts);

        List<Measurement> events = ts.values();
        Assert.assertThat(events.size(), CoreMatchers.is(1));

        Measurement first = events.get(0);

        Assert.assertThat(first.getTime(), CoreMatchers.is(3500L));

        Assert.assertThat(first.getProperty(Measurement.PropertyKey.MAF), CoreMatchers.is(48.0));
        Assert.assertThat(first.getProperty(Measurement.PropertyKey.SPEED), CoreMatchers.is(68.125));

        Assert.assertThat(first.getLatitude(), CoreMatchers.is(52.5));
        Assert.assertThat(first.getLongitude(), CoreMatchers.is(7.25));
    }

}
