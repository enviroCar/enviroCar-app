/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.json.JSONException;

import android.test.AndroidTestCase;

public class ObfuscationTest extends AndroidTestCase {
	
	private static int TARGET_LENGTH = 10;
	
	public void testObfuscation() throws JSONException {
		Track t = createTrack();
		List<Measurement> result = new TrackEncoder().getNonObfuscatedMeasurements(t, true);
		
		Assert.assertTrue("Unexpected element count", result.size() == TARGET_LENGTH);
	}

	private Track createTrack() {
		Track result = new Track("test", new Car(FuelType.DIESEL, "man", "mod", "id", 1234, 123), new DbAdapterMockup());
		
		List<Measurement> measurements = createMeasurements();
		for (Measurement measurement : measurements) {
			result.addMeasurement(measurement);
		}
		
		return result;
	}

	private List<Measurement> createMeasurements() {
		List<Measurement> result = new ArrayList<Measurement>();
		
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis() + 1000*60*100;
		Measurement first = new Measurement(54.0, 9.0);
		first.setTime(start);
		Measurement last = new Measurement(53.0, 8.0);
		last.setTime(end);
		
		result.add(first);
		result.add(createMeasurementNear(first, start+60*1000+1));
		
		createIntermediates(result, first, last);
		
		result.add(createMeasurementNear(last, end-(60*1000+1)));
		result.add(last);
		return result;
	}

	private void createIntermediates(List<Measurement> result, Measurement first,
			Measurement last) {
		double deltaLat = first.getLatitude() - last.getLatitude();
		double deltaLong = first.getLongitude() - last.getLongitude();
		
		long targetTime = first.getTime() + 61*1000;
		
		for (int i = 0; i < TARGET_LENGTH/2; i++) {
			targetTime += 10*1000;
			Measurement m = new Measurement(first.getLatitude() - (deltaLat/2+i/10000), first.getLongitude() - (deltaLong/2+i/10000));
			m.setTime(targetTime);
			result.add(m);
		}
		
		targetTime += 10*1000;
		result.add(createMeasurementNear(first, targetTime));
		
		for (int i = TARGET_LENGTH/2; i < TARGET_LENGTH-1; i++) {
			targetTime += 10*1000;
			Measurement m = new Measurement(first.getLatitude() - (deltaLat/2+i/10000f), first.getLongitude() - (deltaLong/2+i/10000f));
			m.setTime(targetTime);
			result.add(m);
		}
	}

	private Measurement createMeasurementNear(Measurement first, long time) {
		Measurement result = new Measurement(first.getLatitude(), first.getLongitude());
		result.setTime(time);
		return result;
	}

}
