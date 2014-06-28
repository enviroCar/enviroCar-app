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

import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;

import android.test.InstrumentationTestCase;

public class TrackCreateDecisionTest extends InstrumentationTestCase {

	private DbAdapterImpl dbAdapter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		DbAdapterImpl.init(getInstrumentation().getTargetContext());
		this.dbAdapter = (DbAdapterImpl) DbAdapterImpl.instance();
	}
	
	public void testNewTrack() {
		Track t = dbAdapter.createNewTrack();
		t.getTrackId();
		/*
		 * 1.
		 * Collector: get the track to append measurements
		 * dbAdapter: determine if append to old or create new track -> create new
		 */
		
		
		/*
		 * 2.
		 * Collector: add measurments while recording
		 */
		
		
		/*
		 * 3.
		 * Dashboard: User hits "stop" -> should finish track
		 * dbAdapter: finalize current track
		 */
		
		
		/*
		 * 4.
		 * Collector: get the track to append measurements
		 * dbAdapter: do not use previously finalized track -> create new 
		 */
		
		
		/*
		 * 5.
		 * BackgroundServiceImpl: connection is lost -> do NOT finalize track
		 * Collector: connection resumed, get track to append
		 * dbAdapter: if time did not exceed too long or spatial extent changed, use
		 * unfinalized track -> use previous
		 */
	}

}
