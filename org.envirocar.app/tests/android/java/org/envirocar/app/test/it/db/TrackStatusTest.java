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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.core.exception.TrackAlreadyFinishedException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrackStatusTest {

    @Test
	public void testTrackStatusFromDB() throws InstantiationException, TrackAlreadyFinishedException, MeasurementSerializationException {
		DbAdapterImpl dba = new DbAdapterImpl(InstrumentationRegistry.getContext());
		
		Track t = dba.createNewTrack();
		
		Assert.assertTrue(t.getStatus() == TrackStatus.ONGOING);
		
		dba.insertMeasurement(createMeasurement(t));
		Track l = dba.getLastUsedTrack();
		
		Assert.assertTrue(t.getTrackId().equals(l.getTrackId()));
		Assert.assertTrue(l.getStatus() == TrackStatus.ONGOING);
		
		dba.finishCurrentTrack();
		
		Track f = dba.getTrack(t.getTrackId());
		Assert.assertTrue(f.getStatus() == TrackStatus.FINISHED);
		
		dba.deleteTrack(t.getTrackId());
		
		Assert.assertTrue(dba.getTrack(t.getTrackId()) == null);
	}

	private Measurement createMeasurement(Track t) {
		Measurement result = new Measurement(52.0, 7.0);
		result.setTrackId(t.getTrackId());
		return result;
	}

}
