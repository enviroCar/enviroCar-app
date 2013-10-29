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

import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;

public class DbAdapterMockup implements DbAdapter {

	@Override
	@Deprecated
	public DbAdapter open() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void insertNewMeasurement(Measurement measurement) {
		// TODO Auto-generated method stub

	}

	@Override
	public long insertTrack(Track track) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateTrack(Track track) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<Track> getAllTracks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Track getTrack(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTrack(long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void deleteAllTracks() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumberOfStoredTracks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Track getLastUsedTrack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteTrack(long id) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumberOfRemoteTracks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfLocalTracks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteAllLocalTracks() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllRemoteTracks() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Track> getAllLocalTracks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Track createNewTrack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Track finishCurrentTrack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Track> getAllTracks(boolean lazyMeasurements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Measurement> getAllMeasurementsForTrack(Track track) throws TrackWithoutMeasurementsException {
		throw new TrackWithoutMeasurementsException(track);
	}

	@Override
	public Track getTrack(long id, boolean lazyMeasurements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateCarIdOfTracks(String currentId, String newId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insertMeasurement(Measurement measurement)
			throws MeasurementsException {
		// TODO Auto-generated method stub
		
	}


}
