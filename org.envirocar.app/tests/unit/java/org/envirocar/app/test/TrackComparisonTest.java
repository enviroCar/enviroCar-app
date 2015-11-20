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
package org.envirocar.app.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.core.logging.Logger;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.core.exception.TrackAlreadyFinishedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import android.os.Environment;
import android.util.Base64;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class, Logger.class, Base64.class})
public class TrackComparisonTest extends MockingEnvironmentTest {

	@Test
	public void testTracksWithMeasurements() throws TrackAlreadyFinishedException {
		Track t1 = Track.createLocalTrack();
		t1.setMeasurementsAsArrayList(Collections.singletonList(createMeasurement(0)));
		Track t2 = Track.createLocalTrack();
		t2.setMeasurementsAsArrayList(Collections.singletonList(createMeasurement(1)));
		Track t3 = Track.createLocalTrack();
		t3.setMeasurementsAsArrayList(Collections.singletonList(createMeasurement(2)));
		
		List<Track> list = createListAndSort(t1, t2, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(0) == t3);
		Assert.assertTrue("Unexpected position!", list.get(1) == t2);
		Assert.assertTrue("Unexpected position!", list.get(2) == t1);
	}

    @Test
	public void testTracksWithoutMeasurements() {
		Track t1 = Track.createLocalTrack();
		Track t3 = Track.createLocalTrack();
		
		List<Track> list = createListAndSort(t1, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(0) == t1);
		Assert.assertTrue("Unexpected position!", list.get(1) == t3);
	}

    @Test
	public void testOneTrackWithNoMeasurements() throws TrackAlreadyFinishedException {
		Track t1 = Track.createLocalTrack();
		t1.setMeasurementsAsArrayList(Collections.singletonList(createMeasurement(0)));
		Track t2 = Track.createLocalTrack();
		Track t3 = Track.createLocalTrack();
		
		List<Track> list = createListAndSort(t1, t2, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(2) == t1);
	}
	
	private List<Track> createListAndSort(Track... t1) {
		List<Track> list = new ArrayList<Track>();
		
		for (Track track : t1) {
			list.add(track);
		}
		
		Collections.sort(list);
		return list;
	}
	
	private Measurement createMeasurement(int deltaMillis) {
		Measurement result = new Measurement(0.0, 0.0);
		result.setTime(System.currentTimeMillis()+deltaMillis);
		return result;
	}

}
