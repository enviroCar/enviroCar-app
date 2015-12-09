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

import android.os.Environment;
import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class, Logger.class, Base64.class})
public class ObfuscationTest  {
	
//	private static int TARGET_LENGTH = 10;
//
//	@Test
//	public void testObfuscation() throws JSONException, TrackAlreadyFinishedException {
//		Track t = createTrack();
//		List<Measurement> result = new TrackEncoder().getNonObfuscatedMeasurements(t, true);
//
//		Assert.assertTrue("Unexpected element count", result.size() == TARGET_LENGTH);
//	}
//
//	private Track createTrack() throws TrackAlreadyFinishedException {
//		Track result = Track.createLocalTrack();
//		result.setCar(new Car(FuelType.DIESEL, "man", "mod", "id", 1234, 123));
//
//		List<Measurement> measurements = createMeasurements();
//		result.setMeasurementsAsArrayList(measurements);
//
//		return result;
//	}
//
//	private List<Measurement> createMeasurements() {
//		List<Measurement> result = new ArrayList<Measurement>();
//
//		long start = System.currentTimeMillis();
//		long end = System.currentTimeMillis() + 1000*60*100;
//		Measurement first = new Measurement(54.0, 9.0);
//		first.setTime(start);
//		Measurement last = new Measurement(53.0, 8.0);
//		last.setTime(end);
//
//		result.add(first);
//		result.add(createMeasurementNear(first, start+60*1000+1));
//
//		createIntermediates(result, first, last);
//
//		result.add(createMeasurementNear(last, end-(60*1000+1)));
//		result.add(last);
//		return result;
//	}
//
//	private void createIntermediates(List<Measurement> result, Measurement first,
//			Measurement last) {
//		double deltaLat = first.getLatitude() - last.getLatitude();
//		double deltaLong = first.getLongitude() - last.getLongitude();
//
//		long targetTime = first.getTime() + 61*1000;
//
//		for (int i = 0; i < TARGET_LENGTH/2; i++) {
//			targetTime += 10*1000;
//			Measurement m = new Measurement(first.getLatitude() - (deltaLat/2+i/10000), first.getLongitude() - (deltaLong/2+i/10000));
//			m.setTime(targetTime);
//			result.add(m);
//		}
//
//		targetTime += 10*1000;
//		result.add(createMeasurementNear(first, targetTime));
//
//		for (int i = TARGET_LENGTH/2; i < TARGET_LENGTH-1; i++) {
//			targetTime += 10*1000;
//			Measurement m = new Measurement(first.getLatitude() - (deltaLat/2+i/10000f), first.getLongitude() - (deltaLong/2+i/10000f));
//			m.setTime(targetTime);
//			result.add(m);
//		}
//	}
//
//	private Measurement createMeasurementNear(Measurement first, long time) {
//		Measurement result = new Measurement(first.getLatitude(), first.getLongitude());
//		result.setTime(time);
//		return result;
//	}

}
