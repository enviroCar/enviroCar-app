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

import org.envirocar.app.application.CarManager;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.MeasurementSerializationException;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.app.storage.TrackAlreadyFinishedException;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

public class TrackStatusTest extends AndroidTestCase {
	
	public void testTrackStatusFromDB() throws InstantiationException, TrackAlreadyFinishedException, MeasurementSerializationException {
		if (DbAdapterImpl.instance() == null) {
			DbAdapterImpl.init(getContext());
		}
		
		if (CarManager.instance() == null) {
			CarManager.init(PreferenceManager.getDefaultSharedPreferences(getContext()));
		}
		
		Track t = DbAdapterImpl.instance().createNewTrack();
		
		Assert.assertTrue(t.getStatus() == TrackStatus.ONGOING);
		
		DbAdapterImpl.instance().insertMeasurement(createMeasurement(t));
		
		Track l = DbAdapterImpl.instance().getLastUsedTrack();
		
		Assert.assertTrue(t.getTrackId().equals(l.getTrackId()));
		Assert.assertTrue(l.getStatus() == TrackStatus.ONGOING);
		
		DbAdapterImpl.instance().finishCurrentTrack();
		
		Track f = DbAdapterImpl.instance().getTrack(t.getTrackId());
		Assert.assertTrue(f.getStatus() == TrackStatus.FINISHED);
		
		DbAdapterImpl.instance().deleteTrack(t.getTrackId());
		
		Assert.assertTrue(DbAdapterImpl.instance().getTrack(t.getTrackId()) == null);
	}

	private Measurement createMeasurement(Track t) {
		Measurement result = new Measurement(52.0, 7.0);
		result.setTrackId(t.getTrackId());
		return result;
	}

}
