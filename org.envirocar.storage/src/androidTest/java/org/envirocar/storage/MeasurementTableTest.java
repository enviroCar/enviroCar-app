/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.storage;

import android.content.ContentValues;
import android.test.InstrumentationTestCase;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.core.entity.Track;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class MeasurementTableTest extends InstrumentationTestCase {

    @Test
    public void testInfinityValuePassing() throws Exception {
        MeasurementImpl m = new MeasurementImpl();
        m.setTrackId(new Track.TrackId(1));
        m.setProperty(Measurement.PropertyKey.CALCULATED_MAF, Double.POSITIVE_INFINITY);
        m.setProperty(Measurement.PropertyKey.CO2, Double.POSITIVE_INFINITY);
        m.setProperty(Measurement.PropertyKey.SPEED, Double.MAX_VALUE);
        m.setProperty(Measurement.PropertyKey.CONSUMPTION, 1.1);
        ContentValues vals = MeasurementTable.toContentValues(m);

        Object props = vals.get(MeasurementTable.KEY_PROPERTIES);

        JSONObject obj = new JSONObject(props.toString());

        Assert.assertThat(obj.get(Measurement.PropertyKey.CONSUMPTION.name()), CoreMatchers.is(1.1));
        Assert.assertThat(obj.get(Measurement.PropertyKey.SPEED.name()), CoreMatchers.is(Double.MAX_VALUE));
        Assert.assertThat(obj.has(Measurement.PropertyKey.CALCULATED_MAF.name()), CoreMatchers.is(false));
        Assert.assertThat(obj.has(Measurement.PropertyKey.CO2.name()), CoreMatchers.is(false));
    }

}
