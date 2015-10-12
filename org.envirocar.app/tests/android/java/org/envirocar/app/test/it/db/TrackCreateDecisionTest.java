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
package org.envirocar.app.test.it.db;

import junit.framework.Assert;

import org.envirocar.app.model.Position;
import org.envirocar.app.model.TrackId;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackAlreadyFinishedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class TrackCreateDecisionTest {

	private static final int maxTime = 15000;
	private DbAdapterImpl dbAdapter;

	@Before
	protected void setUp() throws Exception {

		this.dbAdapter = new DbAdapterImpl(InstrumentationRegistry.getContext());
		this.dbAdapter.deleteAllTracks();
	}

	@Test
	public void testNewTrack() throws TrackAlreadyFinishedException, MeasurementSerializationException, InterruptedException {
		/*
		 * 1.
		 * Collector: get the track to append measurements
		 * dbAdapter: determine if append to old or create new track -> create new
		 */
		Measurement m = new Measurement(51.00001, 7.00001);
		dbAdapter.insertNewMeasurement(m);
		TrackId active = dbAdapter.getActiveTrackReference(new Position(51.00001, 7.00001));
		
		Measurement first = dbAdapter.getTrack(active).getFirstMeasurement();
		Assert.assertTrue("Measurements are not the same!", m.equals(first));
		
		Measurement m2 = new Measurement(51.00002, 7.00002);
		dbAdapter.insertNewMeasurement(m2);
		
		Measurement last = dbAdapter.getTrack(active).getLastMeasurement();
		Assert.assertTrue("Measurements are not the same!", m2.equals(last));
		
		Thread.sleep(maxTime);
		
		/*
		 * wait the maximum time -> should create a new track
		 */
		m = new Measurement(51.00003, 7.00003);
		dbAdapter.insertNewMeasurement(m);
		
		TrackId newActive = dbAdapter.getActiveTrackReference(new Position(51.00003, 7.00003));
		Assert.assertNotSame("Should be a new track id!", active, newActive);
		
		
		/*
		 * 3.
		 * dbAdapter: finalize current track, next measurement should have a new track
		 */
		dbAdapter.finishCurrentTrack();
		
		TrackId afterFinishing = dbAdapter.getActiveTrackReference(new Position(51.00003, 7.00003));
		Assert.assertNotSame("Should be a new track id!", afterFinishing, newActive);
		
		/*
		 * add the first measurement
		 */
		m = new Measurement(51.95560701047658, 7.626563074300066);
		dbAdapter.insertNewMeasurement(m);
		
		/*
		 * second is close enough in space and time
		 */
		m = new Measurement(51.944327578481714, 7.639612174611539);
		dbAdapter.insertNewMeasurement(m);
		
		TrackId current = dbAdapter.getActiveTrackReference(new Position(m.getLatitude(), m.getLongitude()));
		Assert.assertEquals("Should be the same track id!", current, afterFinishing);
		
		/*
		 * again, first position
		 */
		m = new Measurement(51.95560701047658, 7.626563074300066);
		dbAdapter.insertNewMeasurement(m);
		
		/*
		 * wait the time until the spatial check is jumping in
		 */
		Thread.sleep(maxTime/10);
		
		/*
		 * this measurements is more than 3km away from the previous
		 */
		m = new Measurement(51.934327578481714, 7.659612174611539);
		dbAdapter.insertNewMeasurement(m);
		
		current = dbAdapter.getActiveTrackReference(new Position(m.getLatitude(), m.getLongitude()));
		Assert.assertNotSame("Should be a new track id!", current, afterFinishing);
	}

}
